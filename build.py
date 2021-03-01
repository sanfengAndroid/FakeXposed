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

import argparse
import base64
import errno
import os
import os.path as op
import shutil
import subprocess
import sys


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
    args.abi = args.abi.split(',') if args.abi else ['x86', 'x86_64', 'armeabi-v7a', 'arm64-v8a']
    config['abi'] = list(set(args.abi) & set(['x86', 'x86_64', 'armeabi-v7a', 'arm64-v8a']))
    if not config['abi']:
        error(f'Unsupported build platform type {",".join(args.abi)}')
    config['abi_32'] = list(set(config['abi']) & set(['x86', 'armeabi-v7a']))
    config['abi_64'] = list(set(config['abi']) & set(['x86_64', 'arm64-v8a']))
    global STDOUT
    STDOUT = None if args.verbose else subprocess.DEVNULL
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


def get_build_cmd(args, lib, module, api, merge, abi):
    build_type = 'Release' if args.release else 'Debug'
    return [gradlew, f'{module}:externalNativeBuild{build_type}' if lib else f'{module}:assemble{build_type}', f'-PbuildApi={api}',
            f'-PconfigPath={op.abspath(args.config)}', f'-PmergeBuild={merge}', f'-PlogLevel={args.log}', '-Pabis=' + ",".join(abi)]


def get_apk_name(type):
    dir = op.join('app', 'build', 'outputs', 'apk', type)
    for file in os.listdir(dir):
        if file.endswith('.apk'):
            return file
    return ""


def build_all(args):
    cleanup(args)
    for api in range(21, 31):
        if api == 25:
            continue
        header(f'building api {api}')
        module = 'fake-linker' if api != 21 and api != 30 else 'app'
        proc = execv(get_build_cmd(args, True, module, api, args.merge, config['abi']))
        if proc.returncode != 0:
            error(f'Build external native {api} failed!')

    for target in [['x86'], ['armeabi-v7a']] if args.merge else [['x86', 'x86_64'], ['armeabi-v7a', 'arm64-v8a']]:
        clean_build()
        abis = config['abi_32'] if args.merge else config['abi']
        abis = list(set(abis) & set(target))
        if abis:
            proc = execv(get_build_cmd(args, False, 'app', 30, args.merge, abis))
            if proc.returncode != 0:
                error(f'Build app failed!')

            type = 'release' if args.release else 'debug'
            name = get_apk_name(type)
            source = op.join('app', 'build', 'outputs', 'apk', type, name)
            target = op.join('out', name)
            mv(source, target)
    clean_generate()
    header(f'build fully app successfully.')


def build_api(args):
    clean_build()
    if args.level < 21 or args.level > 30:
        error(f'unsupported api level: {args.level}')
    if args.level == 25:
        args.level = 24
        header('Warning:compiled api version 25 uses 24 instead.')
    if args.merge:
        if config['abi_64']:
            proc = execv(get_build_cmd(args, True, 'app', args.level, True, config['abi_64']))
            if proc != 0 and proc.returncode != 0:
                error(f'Build special api {args.level} 64 bit external native failed!')
            clean_build()

    proc = execv(get_build_cmd(args, False, 'app', args.level, args.merge, config['abi_32'] if args.merge else config['abi']))
    if proc.returncode != 0:
        error(f'Build special api {args.level} assemble failed!')

    clean_generate()
    header(f'build the specified api {args.level} successfully.')


def build_library(args):
    clean_build()
    header(args.level)
    for level in args.level:
        if level < 21 or level > 30:
            error(f'unsupported api level: {args.level}')
        if level == 25:
            header('Warning:compile incompatible versions of ndk: 25')
            level = 24
        proc = execv(get_build_cmd(args, True, 'app', level, args.merge, config['abi']))
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
parser.add_argument('-l', '--log', default=2, type=int,
                    help='configure the log level of the application')
parser.add_argument('-s', '--cstr',
                    help='string configuration base64 string')
parser.add_argument('-f', '--signfile',
                    help='signature file base64 string')
parser.add_argument('-a', '--abi', help='build the specified abi')

parser.add_argument('-t', '--target', default=29, type=int,
                    help='build the specified api targetSdk')
subparsers = parser.add_subparsers(title='actions')

all_parser = subparsers.add_parser('all', help='fully build')
all_parser.set_defaults(func=build_all)

clean_parser = subparsers.add_parser('clean', help='cleanup native')
clean_parser.set_defaults(func=cleanup)

api_parser = subparsers.add_parser('api', help='build the specified api')
api_parser.add_argument('level', type=int)
api_parser.set_defaults(func=build_api)

lib_parser = subparsers.add_parser(
    'lib', help='build the specified api library to jniFolder')
lib_parser.add_argument('level', type=int, nargs='+')
lib_parser.set_defaults(func=build_library)

if len(sys.argv) == 1:
    parser.print_help()
    sys.exit(1)

args = parser.parse_args()
load_config(args)

args.func(args)
