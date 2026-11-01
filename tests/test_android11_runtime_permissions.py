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

    def test_legacy_books_library_routes_are_removed_from_reader_first_home(self):
        app_source = (ROOT / "unified-app/app/src/main/java/com/mashxall/unified/MainActivity.kt").read_text(encoding="utf-8")
        enum_body = app_source.split("enum class AppScreen", 1)[1].split("}", 1)[0]
        home_modules = app_source.split("private val homeModules", 1)[1].split("private val textMap", 1)[0]
        self.assertNotRegex(enum_body, r"\b(?:BOOKS|LIBRARY)\s*\(")
        self.assertNotRegex(home_modules, r"HomeModule\(AppScreen\.(?:BOOKS|LIBRARY)\b")


if __name__ == "__main__":
    unittest.main()
