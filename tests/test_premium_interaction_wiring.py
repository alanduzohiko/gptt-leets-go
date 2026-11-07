from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
APP_SCREENS = (ROOT / "unified-app/app/src/main/java/com/mashxall/unified/AppScreens.kt").read_text(encoding="utf-8")
MOTION = (ROOT / "unified-app/app/src/main/java/com/mashxall/unified/PremiumMotion.kt").read_text(encoding="utf-8")
MAIN_ACTIVITY = (ROOT / "unified-app/app/src/main/java/com/mashxall/unified/MainActivity.kt").read_text(encoding="utf-8")


class PremiumInteractionWiringTests(unittest.TestCase):
    def test_screen_actions_use_shared_native_controls(self):
        self.assertIn("PremiumOutlinedButton(", APP_SCREENS)
        # The Quran-reader text-size control was intentionally removed; background
        # strength/speed sliders remain native controls on the main settings screen.
        self.assertIn("PremiumSlider(", MAIN_ACTIVITY)
        self.assertNotIn("VERSE PANEL TEXT SIZE", APP_SCREENS)
        self.assertNotIn('onValueChange = { verseTextScale = it', APP_SCREENS)
        categories = APP_SCREENS.split("val categories = listOf(", 1)[1].split("LazyColumn", 1)[0]
        self.assertNotIn('Triple("font"', categories)
        self.assertIn("quranTextFontOptions", APP_SCREENS)
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
        self.assertIn("visible = true", reader)
        self.assertIn("slideInVertically(tween(210))", reader)
        self.assertIn("scaleIn(initialScale = 0.98f", reader)
        self.assertIn("Opening the page-based Mushaf", reader)


    def test_shared_section_hero_decorative_layer_does_not_expand_to_full_screen(self):
        hero = MOTION.split("internal fun PremiumSectionHero(", 1)[1].split(
            "/**\n * A restrained orbit around the Qibla compass.", 1
        )[0]
        self.assertIn("modifier = Modifier.matchParentSize()", hero)
        self.assertNotIn("modifier = Modifier.fillMaxSize()", hero)
        self.assertIn("Modifier.fillMaxWidth()", hero)

    def test_quran_index_places_surah_rows_before_library_progress_summary(self):
        reader = APP_SCREENS.split("} else if (showSurahList) {", 1)[1].split(
            "} else if (openingMushaf)", 1
        )[0]
        self.assertLess(
            reader.index("itemsIndexed(filteredSurahs"),
            reader.index('item(key = "quran-reading-progress-footer")')
        )
        self.assertNotIn("QuranReadingProgressCard(", reader.split("SearchBox(", 1)[0])

    def test_quran_index_searches_and_opens_matching_ayahs(self):
        self.assertIn("ContentRepository.searchAyahs(", APP_SCREENS)
        self.assertIn("quran-ayah-search-heading", APP_SCREENS)
        self.assertIn('onOpenVerse(result.surah, result.number)', APP_SCREENS)
        self.assertIn("Search surahs or Quran verses", APP_SCREENS)

    def test_settings_expose_about_share_and_direct_email_contact(self):
        main_path = ROOT / "unified-app/app/src/main/java/com/mashxall/unified/MainActivity.kt"
        main = main_path.read_text(encoding="utf-8")
        self.assertIn('"about_app", "about_data" -> AboutMashxallScreen', main)
        self.assertIn("onShareApp = { shareMashxallApp(context, language) }", main)
        self.assertIn("aland.mashxall@gmail.com", main)
        self.assertIn("Intent.ACTION_SENDTO", main)
        self.assertNotIn("Data sources & rights", main)

    def test_shared_live_background_has_persistent_style_strength_and_speed_controls(self):
        main_path = ROOT / "unified-app/app/src/main/java/com/mashxall/unified/MainActivity.kt"
        main = main_path.read_text(encoding="utf-8")
        self.assertIn("if (!immersiveMushaf && shouldShowAmbientBackdrop(backgroundStyle))", main)
        self.assertIn("shouldAnimateAmbientBackdrop(visibleScreen, motionAllowed, backgroundStyle)", main)
        self.assertIn("ambientBackdropIntensityForScreen(visibleScreen, backgroundIntensity)", main)
        self.assertIn('listOf("ambient", "orbit", "ornament", "off").chunked(2)', main)
        self.assertIn("modifier = Modifier.weight(1f)", main)
        self.assertIn("accentForPreview(backgroundStyle)", main)
        self.assertIn("onBackgroundIntensityCommit", main)
        self.assertIn("onBackgroundSpeedCommit", main)
        self.assertIn('putString("background_style", backgroundStyle)', main)
        self.assertIn('putFloat("background_intensity", backgroundIntensity)', main)
        self.assertIn('putFloat("background_speed", backgroundSpeed)', main)

    def test_section_heroes_pulse_and_button_press_motion_is_control_specific(self):
        self.assertIn("pulseIcon: Boolean = true", MOTION)
        self.assertIn("internal fun premiumControlPressRotationDegrees(", MOTION)
        self.assertIn("premiumControlPressRotationDegrees(role, pressedScale)", MOTION)
        self.assertIn("label = \"Mashxall section hero breathing light\"", MOTION)

    def test_visible_screen_ornaments_cover_every_destination_and_keep_mushaf_subtle(self):
        main_path = ROOT / "unified-app/app/src/main/java/com/mashxall/unified/MainActivity.kt"
        main = main_path.read_text(encoding="utf-8")

        self.assertIn('itemKey = "destination-$visibleScreen"', main)
        self.assertIn("PremiumScreenOrnamentOverlay(", main)
        self.assertIn("premiumScreenOrnamentIntensity(", main)
        self.assertIn("if (shouldShowAmbientBackdrop(backgroundStyle))", main)
        self.assertIn("immersiveMushaf = immersiveMushaf", main)
        self.assertIn("internal fun PremiumScreenOrnamentOverlay(", MOTION)
        self.assertIn('label = "Mashxall edge ornament orbit"', MOTION)
        self.assertIn("drawArc(", MOTION)
        self.assertIn("premiumOrnamentVertex(", MOTION)

    def test_background_motion_is_single_transition_and_respects_reduce_motion(self):
        self.assertIn('rememberInfiniteTransition(label = "Mashxall shared ambient atmosphere")', MOTION)
        self.assertIn("val runMotion = animated && motionAllowed", MOTION)
        self.assertIn("normalizeAmbientBackdropSpeed(speed)", MOTION)
        self.assertIn("ambientMotionDurationMillis", MOTION)

    def test_choice_icon_response_and_quran_ornament_respect_motion_policy(self):
        self.assertIn('label = "Mashxall choice pill icon spring"', MOTION)
        self.assertIn('label = "Mashxall choice pill icon turn"', MOTION)
        quran = APP_SCREENS.split("fun QuranSurahWelcomeCard(", 1)[1].split(
            "@Composable\\nprivate fun QuranReadingProgressCard(", 1
        )[0]
        self.assertIn("val ornamentMotion = if (motionAllowed)", quran)
        self.assertIn("else {\n        0f to 1f\n    }", quran)

    def test_settings_entrance_animation_respects_reduce_motion(self):
        main_path = ROOT / "unified-app/app/src/main/java/com/mashxall/unified/MainActivity.kt"
        main = main_path.read_text(encoding="utf-8")
        settings = main.split("private fun SettingsScreen(", 1)[1]
        self.assertIn("val motionAllowed = remember(context, reduceMotion)", settings)
        self.assertIn("enter = if (motionAllowed)", settings)
        self.assertIn("fadeIn(animationSpec = tween(0))", settings)

    def test_selected_icon_jewels_follow_reduce_motion(self):
        self.assertEqual(2, MOTION.count("enter = if (motionAllowed) scaleIn("))


if __name__ == "__main__":
    unittest.main()
