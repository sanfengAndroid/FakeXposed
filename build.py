#!usr/bin/env python3
#  Copyright (c) 2021 FakeXposed by sanfengAndroid.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

import sys
import os
import subprocess
import os.path as op
import argparse
import shutil
import errno
import base64


def error(str):
    if is_ci:
        print(f'\n ! {str}\n')
    else:
        print(f'\n\033[41m{str}\033[0m\n')
    sys.exit(1)


def header(str):
    if is_ci:
        print(f'\n{str}\n')
    else:
        print(f'\n\033[44m{str}\033[0m\n')


def vprint(str):
    if args.verbose:
        print(str)

def mv(source, target):
    try:
        shutil.move(source, target)
        vprint(f'mv {source} -> {target}')
    except:
        pass

is_windows = os.name == 'nt'
is_ci = 'CI' in os.environ and os.environ['CI'] == 'true'

if not is_ci and is_windows:
    import colorama
    colorama.init()

if not sys.version_info >= (3, 6):
    error('Requires Python 3.6+')

if 'ANDROID_SDK_ROOT' not in os.environ:
    error('Please add Android SDK path to ANDROID_SDK_ROOT environment variable!')

try:
    subprocess.run(['javac', '-version'],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
except FileNotFoundError:
    error('Please install JDK and make sure it in PATH')

gradlew = op.join('.', 'gradlew' + ('.bat' if is_windows else ''))


def rm(file):
    try:
        os.remove(file)
        vprint(f'rm {file}')
    except OSError as e:
        if e.errno != errno.ENOENT:
            raise

def mkdir(path, mode=0o755):
    try:
        os.mkdir(path, mode)
    except:
        pass

def rm_rf(path):
    vprint(f'rm -rf {path}')
    shutil.rmtree(path, ignore_errors=True)

config = {}
STDOUT = None


def execv(cmd):
    return subprocess.run(cmd, stdout=STDOUT)

def parse_props(file):
    props = {}
    with open(file, 'r') as f:
        for line in [l.strip(' \t\r\n') for l in f]:
            if line.startswith('#') or len(line) == 0:
                continue
            prop = line.split('=')
            if len(prop) != 2:
                continue
            value = prop[1].strip(' \t\r\n')
            if len(value) == 0:
                continue
            props[prop[0].strip(' \t\r\n')] = value
    return props


def load_config(args):
    # profile first
    if op.exists(args.config):
        config.update(parse_props(args.config))
    elif args.cstr:
        # string config save to file
        cstr = base64.b64decode(args.cstr)
        f = open('config.prop', 'w', encoding='utf-8')
        f.write(str(cstr, encoding='utf-8'))
        f.close()
        args.config = 'config.prop'
        config.update(parse_props(args.config))
        config['copy_config'] = args.config
    if args.signfile:
        sf = base64.b64decode(args.signfile)
        f = open('sign.jks', 'wb')
        f.write(sf)
        f.close()
        config['copy_sign'] = op.abspath('sign.jks')
    global STDOUT
    STDOUT = None if args.verbose else subprocess.DEVNULL
    if is_ci:
        mkdir('out')


def clean_build():
    rm_rf(op.join('app', 'build'))
    rm_rf(op.join('app', '.cxx'))
    rm_rf(op.join('fake-linker', 'build'))
    rm_rf(op.join('fake-linker', '.cxx'))


def cleanup(args):
    execv([gradlew, 'clean'])
    rm_rf(op.join('app', 'src', 'main', 'libs'))
    rm_rf(op.join('fake-linker', 'src', 'main', 'libs'))

def clean_generate():
    if 'copy_config' in config:
        rm(config['copy_config'])
    if 'copy_sign' in config:
        rm(config['copy_sign'])

def build_all(args):
    cleanup(args)
    build_type = 'Release' if args.release else 'Debug'
    for api in range(21, 31):
        # api 25 needs to switch ndk low version
        if api == 25:
            continue
        header(f'building api {api}')
        module = 'fake-linker' if api != 23 and api != 30 else 'app'
        proc = execv([gradlew, f'{module}:externalNativeBuild{build_type}', f'-PbuildApi={api}',
                      '-PconfigPath=' + op.abspath(args.config), f'-PmergeBuild={args.merge}', f'-PlogLevel={args.log}'])
        if proc.returncode != 0:
            error(f'Build external native {api} failed!')
    if args.merge:
        clean_build()
        proc = execv([gradlew, f'app:assemble{build_type}', '-PbuildApi=30', '-PmergeBuild=true', '-Pabis=armeabi-v7a,x86',
                      '-PconfigPath=' + op.abspath(args.config), f'-PlogLevel={args.log}'])
    else:
        proc = execv([gradlew, f'app:assemble{build_type}', '-PbuildApi=30',
                      '-PmergeBuild=false', '-PconfigPath=' + op.abspath(args.config), f'-PlogLevel={args.log}'])
    if proc.returncode != 0:
        error(f'Build app failed!')
    clean_generate()
    if is_ci:
        type = 'release' if args.release else 'debug'
        name = 'app-release.apk' if args.release else 'app-debug.apk'
        source = op.join('app','build','outputs','apk', type, name)
        target = op.join('out', name)
        mv(source, target)
    header(f'build fully app successfully.')

def build_api(args):
    clean_build()
    build_type = 'Release' if args.release else 'Debug'
    if args.level < 21 or args.level > 30:
        error(f'unsupported api level: {args.level}')
    if args.level == 25:
        args.level = 24
        header('Warning:compiled api version 25 uses 24 instead')
    if args.merge:
        proc = execv([gradlew,  f'app:externalNativeBuild{build_type}',
                      f'-PbuildApi={args.level}', '-PmergeBuild=true', '-PconfigPath=' + op.abspath(args.config), '-Pabis=arm64-v8a,x86_64', f'-PlogLevel={args.log}'])
        if proc.returncode != 0:
            error(f'Build special api {args.level} external native failed!')
        clean_build()
        proc = execv([gradlew, f'app:assemble{build_type}', f'-PbuildApi={args.level}', '-PmergeBuild=true', '-Pabis=armeabi-v7a,x86',
                      '-PconfigPath=' + op.abspath(args.config), f'-PlogLevel={args.log}'])
    else:
        proc = execv([gradlew, f'app:assemble{build_type}', f'-PbuildApi={args.level}',
                      '-PmergeBuild=false', '-PconfigPath=' + op.abspath(args.config), f'-PlogLevel={args.log}'])
    if proc.returncode != 0:
        error(f'Build special api {args.level} assemble failed!')
    clean_generate()
    header(f'build the specified api {args.level} successfully.')

def build_library(args):
    clean_build()
    build_type = 'Release' if args.release else 'Debug'
    header(args.level)
    for level in args.level:
        if level < 21 or level > 30:
            error(f'unsupported api level: {args.level}')
        if level == 25:
            header('Warning:compile incompatible versions of ndk: 25')
        proc = execv([gradlew, f'app:externalNativeBuild{build_type}',f'-PbuildApi={level}',f'-PmergeBuild={args.merge}', '-PconfigPath=' + op.abspath(args.config), f'-PlogLevel={args.log}'])
        if proc.returncode != 0:
            error(f'Build special api {level} library failed!')
    clean_generate()
    clean_build()
    header(f'build the specified api {args.level} library successfully.')

parser = argparse.ArgumentParser(description='FakeXposed build script')
parser.set_defaults(func=lambda x: None)
parser.add_argument('-r', '--release', action='store_true',
                    help='compile in release mode')
parser.add_argument('-v', '--verbose', action='store_true',
                    help='verbose output')
parser.add_argument('-m', '--merge', action='store_true',
                    help='merging 64-bit libraries to 32-bit libraries')
parser.add_argument('-c', '--config', default='local.properties',
                    help='custom config file (default: local.properties)')
parser.add_argument('-l','--log', default=2, type=int, help='configure the log level of the application')
parser.add_argument('-s', '--cstr', 
                    help='string configuration base64 string')
parser.add_argument('-f', '--signfile', 
                    help='signature file base64 string')
parser.add_argument('-t','--target', default=29,type=int, help='build the specified api targetSdk')
subparsers = parser.add_subparsers(title='actions')

all_parser = subparsers.add_parser('all', help='fully build')
all_parser.set_defaults(func=build_all)

clean_parser = subparsers.add_parser('clean', help='cleanup native')
clean_parser.set_defaults(func=cleanup)

api_parser = subparsers.add_parser('api', help='build the specified api')
api_parser.add_argument('level', type=int)
api_parser.set_defaults(func=build_api)

lib_parser = subparsers.add_parser('lib', help='build the specified api library to jniFolder')
lib_parser.add_argument('level', type=int, nargs='+')
lib_parser.set_defaults(func=build_library)

if len(sys.argv) == 1:
    parser.print_help()
    sys.exit(1)

args = parser.parse_args()
load_config(args)


args.func(args)
