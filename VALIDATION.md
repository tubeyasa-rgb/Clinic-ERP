# Validation

Final command (exit 0):

```sh
cd /workspace/Clinic-ERP
python .cloud/build.py :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

- Debug APK built successfully and APK v2 signature verified with Android apksigner.
- 13 unit tests passed: 12 regression tests covering appointment date/time/UTC handling, invalid input, Odoo optional text values, patient identity and JSON-RPC errors; plus the original example test. No failures, errors or skipped unit tests.
- Android Lint: no errors; 17 warnings. These include dependency updates, target API age, formatting/API design and unused resources. The initialization-provider warning refers to an omitted source attribute; the merged manifest explicitly sets exported=false.
- Gradle Wrapper distribution SHA-256 verification retained. JDK archive SHA-256 and Android command-line tools archive checksums verified against their official downloads. TLS verification remains enabled using the environment's trusted system certificate store.
- Reviewed Odoo upstream 19.0 and 20.0 source for JSON-RPC routing, permissions and password-change signatures.

Not executed: live Odoo login/CRUD/calendar integration, device instrumentation, UI interaction on a phone/emulator, or a signed production release. No Odoo credentials or patient data were provided. Debug APK is for testing and uses a newly generated debug signing key; an existing installation signed with a different key cannot be updated directly with this APK.

The source is in /workspace/Clinic-ERP. The source and downloadable test artifacts are prepared for publication to GitHub at the user’s request. Environment install_script and start_skill were saved as a draft; saving does not publish the environment or validate restoration in a fresh task.
