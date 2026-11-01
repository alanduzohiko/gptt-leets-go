from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SMOKE = (ROOT / "tools/runtime-smoke-android11.sh").read_text(encoding="utf-8")


class Android11RuntimePermissionSmokeTests(unittest.TestCase):
    def test_runtime_permissions_are_pregranted_and_verified(self):
        self.assertIn("pm grant com.mashxall.unified", SMOKE)
        self.assertIn("pm check-permission", SMOKE)
        self.assertIn("location-permissions.txt", SMOKE)

    def test_location_permission_dialog_has_an_accessible_allow_path(self):
        self.assertIn("permission_allow_foreground_only_button", SMOKE)
        self.assertIn("WHILE USING THE APP", SMOKE)
        self.assertIn("ACCESS_FINE_LOCATION", SMOKE)


if __name__ == "__main__":
    unittest.main()
