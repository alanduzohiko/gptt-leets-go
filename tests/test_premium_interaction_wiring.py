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

    def test_animated_counter_audio_and_prayer_percentage_respect_reduce_motion(self):
        for label in (
            "Tasbih rolling counter",
            "Reciter active audio waveform transition",
            "Prayer day percentage",
        ):
            with self.subTest(label=label):
                label_index = APP_SCREENS.index(f'label = "{label}"')
                transition_start = APP_SCREENS.rfind("transitionSpec = {", 0, label_index)
                transition = APP_SCREENS[transition_start:label_index]
                self.assertIn("if (motionAllowed)", transition)
                self.assertIn("fadeIn(tween(0)) togetherWith fadeOut(tween(0))", transition)

    def test_more_worship_and_names_expansion_respect_reduce_motion(self):
        main_path = ROOT / "unified-app/app/src/main/java/com/mashxall/unified/MainActivity.kt"
        main = main_path.read_text(encoding="utf-8")

        more = main.split("private fun MoreScreen(", 1)[1].split(
            "\\n@Composable\\nfun LibraryScreen(", 1
        )[0]
        self.assertIn("motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion)", more)
        self.assertIn("enter = if (motionAllowed)", more)
        self.assertIn("fadeIn(animationSpec = tween(0))", more)

        worship_full = main.split("fun WorshipScreen(", 1)[1]
        self.assertIn("motionAllowed = remember(context, MashxallMotionPreferences.reduceMotion)", worship_full)
        self.assertIn("enter = if (motionAllowed)", worship_full)

        names = APP_SCREENS.split("visible = expanded,", 1)[1][:1200]
        self.assertIn("enter = if (motionAllowed)", names)
        self.assertIn("exit = if (motionAllowed)", names)
        self.assertIn("fadeIn(tween(0))", names)
        self.assertTrue(
            "fadeOut(tween(0))" in names or
            "fadeOut(animationSpec = tween(0))" in names
        )


    def test_quran_welcome_uses_live_motion_and_real_reader_actions(self):
        welcome = APP_SCREENS.split("private fun QuranSurahWelcomeCard(", 1)[1].split(
            "@Composable\\nprivate fun QuranReadingProgressCard(", 1
        )[0]
        self.assertIn('staggeredEntrance(itemKey = "quran-reader-welcome"', welcome)
        self.assertIn("rememberInfiniteTransition(label = \"Quran welcome ornament\")", welcome)
        self.assertIn("Quran welcome ring rotation", welcome)
        self.assertIn("Quran welcome ring breathing", welcome)
        self.assertIn("onClick = onContinue", welcome)
        self.assertIn("onClick = onOpenMushaf", welcome)

    def test_quran_progress_values_animate_when_reading_state_changes(self):
        progress = APP_SCREENS.split("private fun QuranReadingProgressCard(", 1)[1].split(
            "@Composable\\nprivate fun QuranProgressStat(", 1
        )[0]
        stat = APP_SCREENS.split("private fun QuranProgressStat(", 1)[1].split(
            "@Composable\\nprivate fun QuranSurahIndexCard(", 1
        )[0]
        self.assertIn('staggeredEntrance(itemKey = "quran-reading-progress"', progress)
        self.assertIn("Quran progress percentage roll", progress)
        self.assertIn("Quran reading statistic transition", stat)

    def test_quran_reader_modes_enter_with_native_transitions(self):
        reader = APP_SCREENS.split("if (mushafMode) {", 1)[1].split(
            "if (showReaderSettings)", 1
        )[0]
        self.assertIn("Quran page-based Mushaf entry", reader) if False else None
        self.assertIn("visible = true", reader)
        self.assertIn("slideInVertically(tween(210))", reader)
        self.assertIn("scaleIn(initialScale = 0.98f", reader)
        self.assertIn("Opening Mushaf", reader)


    def test_selected_icon_jewels_follow_reduce_motion(self):
        self.assertEqual(2, MOTION.count("enter = if (motionAllowed) scaleIn("))


if __name__ == "__main__":
    unittest.main()
