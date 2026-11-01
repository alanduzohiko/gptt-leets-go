from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
APP_SCREENS = (ROOT / "unified-app/app/src/main/java/com/mashxall/unified/AppScreens.kt").read_text(encoding="utf-8")
MOTION = (ROOT / "unified-app/app/src/main/java/com/mashxall/unified/PremiumMotion.kt").read_text(encoding="utf-8")


class PremiumInteractionWiringTests(unittest.TestCase):
    def test_screen_actions_use_shared_native_controls(self):
        self.assertIn("PremiumOutlinedButton(", APP_SCREENS)
        self.assertIn("PremiumSlider(", APP_SCREENS)
        self.assertNotIn("\n                OutlinedButton(", APP_SCREENS)
        self.assertNotIn("\n                            Slider(", APP_SCREENS)

    def test_screen_loading_states_use_motion_aware_ring(self):
        self.assertGreaterEqual(APP_SCREENS.count("PremiumLoadingIndicator("), 10)
        self.assertNotIn("CircularProgressIndicator(", APP_SCREENS)
        self.assertIn("progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate", MOTION)
        self.assertNotIn("CircularProgressIndicator(", MOTION)

    def test_haptics_have_one_preference_aware_gateway(self):
        self.assertEqual(1, MOTION.count("performHapticFeedback("))
        self.assertIn("shouldPerformMashxallHaptic(MashxallMotionPreferences.hapticsEnabled)", MOTION)
        self.assertNotIn("performHapticFeedback(", APP_SCREENS)

    def test_mushaf_scrubber_uses_shared_slider_and_haptic_policy(self):
        scrubber = MOTION.split("internal fun PremiumPageScrubber(", 1)[1]
        self.assertIn("PremiumSlider(", scrubber)
        self.assertNotIn(".performHapticFeedback(", scrubber)

    def test_selected_icon_jewels_follow_reduce_motion(self):
        self.assertEqual(2, MOTION.count("enter = if (motionAllowed) scaleIn("))


if __name__ == "__main__":
    unittest.main()
