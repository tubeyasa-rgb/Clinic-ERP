#!/usr/bin/env python3
"""Build in the prepared cloud snapshot without changing shell or credential files."""
import os
from pathlib import Path
import subprocess
import sys
from urllib.parse import urlsplit

root = Path(__file__).resolve().parents[1]
configuration = os.environ.copy()
candidates = [Path(configuration.get('JAVA_HOME', '/nonexistent'))]
candidates += sorted(Path('/workspace/jdk').glob('jdk-21*'), reverse=True)
candidates.append(Path('/usr/lib/jvm/java-21-openjdk-amd64'))
jdk = next((path for path in candidates if (path / 'bin/javac').is_file()), None)
if jdk is None:
    sys.exit('A full JDK 21 is required. Configure JAVA_HOME or restore the prepared environment snapshot.')
configuration['JAVA_HOME'] = str(jdk)
configuration.setdefault('ANDROID_HOME', '/workspace/android-sdk')
configuration.setdefault('ANDROID_USER_HOME', '/workspace/.android')
configuration.setdefault('GRADLE_USER_HOME', '/workspace/.gradle-cache')
Path(configuration['ANDROID_USER_HOME']).mkdir(parents=True, exist_ok=True)
flags = ['-Duser.home=/workspace/.java-user']
proxy = urlsplit(configuration.get('HTTPS_PROXY') or configuration.get('https_proxy') or '')
if proxy.hostname:
    for scheme in ('http', 'https'):
        flags += [f'-D{scheme}.proxyHost={proxy.hostname}', f'-D{scheme}.proxyPort={proxy.port or 80}']
trust_store = Path('/etc/ssl/certs/java/cacerts')
if trust_store.is_file():
    flags += [f'-Djavax.net.ssl.trustStore={trust_store}']
arguments = sys.argv[1:] or [':app:testDebugUnitTest', ':app:assembleDebug']
sys.exit(subprocess.call([str(root / 'gradlew'), *flags, '--no-daemon', '--max-workers=2', *arguments], cwd=root, env=configuration))
