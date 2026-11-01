import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MAIN_ACTIVITY = ROOT / "unified-app/app/src/main/java/com/mashxall/unified/MainActivity.kt"


class HomePrayerPriorityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        source = MAIN_ACTIVITY.read_text(encoding="utf-8")
        cls.home = source.split("private fun HomeScreen(", 1)[1].split(
            "\n@Composable\nfun LibraryScreen(", 1
        )[0]

    def test_prayer_timetable_is_before_quick_access(self):
        quick_access = self.home.index('Text(tr(language, "quick_access")')
        self.assertLess(self.home.index("HomePrayerCountdown("), quick_access)
        self.assertLess(self.home.index("HomePrayerJourneyStrip("), quick_access)

    def test_quick_access_is_directly_ahead_of_welcome_and_resume_cards(self):
        quick_access = self.home.index('Text(tr(language, "quick_access")')
        welcome = self.home.index("PremiumHomeHero(", quick_access)
        resume = self.home.index("HomeReadingResumeCard(", welcome)
        self.assertLess(quick_access, welcome)
        self.assertLess(welcome, resume)
        self.assertIn("onResumeReading = onResumeReading", self.home[resume:])

    def test_quick_access_stays_inside_home_screen(self):
        self.assertIn("homeModules.chunked(2)", self.home)
        self.assertIn("HomePrayerCountdown(", self.home)
        self.assertIn("HomeReadingResumeCard(", self.home)

    def test_resume_card_uses_saved_reader_mode_and_position(self):
        source = MAIN_ACTIVITY.read_text(encoding="utf-8")
        self.assertIn("homeQuranResumeDetails(", source)
        self.assertIn('prefs.getString("quran_reader_mode", "verse")', source)
        self.assertIn('prefs.getInt("last_ayah_number", 1)', source)
        self.assertIn('prefs.getInt("last_mushaf_page", 1)', source)


if __name__ == "__main__":
    unittest.main()
