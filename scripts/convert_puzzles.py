from pathlib import Path
from typing import Dict, List, Any
from zipfile import ZipFile
from dataclasses import dataclass
import re
import sys
import os


def exit_with_usage():
    print(
        f"""Converts a puzzle folder in the old import format to the new format.

Usage: {os.path.basename(sys.argv[0])} input_path output_path
    input_path: directory or zip file containing puzzle chapters in the old format
    output_path: directory in which to store the converted puzzle files. should be empty""",
        file=sys.stderr,
    )
    sys.exit(1)


@dataclass
class TextFile:
    path: Path
    content: str


def read_zip(zip_path: Path) -> List[TextFile]:
    files = []
    with ZipFile(zip_path) as zip:
        for path in zip.namelist():
            with zip.open(path) as file:
                content = file.read().decode("UTF-8")
                files.append(TextFile(path=Path(path), content=content))
    return files


def read_properties_file(content: str) -> Dict[str, Any]:
    lines = content.splitlines()
    properties = {}

    for line in lines:
        line = line.strip()

        if not line or line.isspace() or line.startswith("#"):
            continue

        name, value = line.split(sep="=", maxsplit=1)
        name = name.strip()
        value = value.strip()

        if value.isnumeric():
            value = int(value)

        properties[name] = value
    return properties


def format_chapter_dir(title: str, position: int) -> str:
    dir_name = f"Chapter {position:02d} - {title}"
    return re.sub(r"^[ .]|[/<>:\"\\|?*]+|[ .]$", "_", dir_name)


def format_puzzle_dir(title: str, position: int) -> str:
    dir_name = f"Puzzle {position:02d} - {title}"
    return re.sub(r"^[ .]|[/<>:\"\\|?*]+|[ .]$", "_", dir_name)


class PuzzleConverter:
    def __init__(self, files: List[TextFile], output_dir: Path):
        self.files = files
        self.output_dir = output_dir

        # Map (alias, position) to test file
        self.tests = {
            (file.path.parts[-3], int(file.path.parts[-2])): file
            for file in files
            if len(file.path.parts) >= 4
            and file.path.parts[-4] == "tests"
            and file.path.name.endswith(".java")
        }

        # Map (alias, position) to mutant file
        self.mutants = {
            (file.path.parts[-3], int(file.path.parts[-2])): file
            for file in files
            if len(file.path.parts) >= 4
            and file.path.parts[-4] == "mutants"
            and file.path.name.endswith(".java")
        }

        # Map alias to cut file
        self.cuts = {
            file.path.parts[-2]: file
            for file in files
            if len(file.path.parts) >= 3
            and file.path.parts[-3] == "cuts"
            and file.path.name.endswith(".java")
        }

        def add_props(props, **kwargs):
            for key, value in kwargs.items():
                props[key] = value
            return props

        # Contents of the chapter .properties files
        self.chapters = [
            add_props(read_properties_file(file.content), file=file)
            for file in files
            if len(file.path.parts) >= 2
            and file.path.parts[-2] == "puzzleChapters"
            and file.path.name.endswith(".properties")
        ]
        # Will map chapter IDs to their directory
        self.chapter_dirs = {}

        # Contents of the puzzle .properties files
        self.puzzles = [
            add_props(
                read_properties_file(file.content), file=file, alias=file.path.parts[-2]
            )
            for file in files
            if len(file.path.parts) >= 3
            and file.path.parts[-3] == "puzzles"
            and file.path.parts[-2] != "puzzleChapters"
            and file.path.name.endswith(".properties")
        ]

    def convert(self) -> None:
        self.convert_chapters()
        self.convert_puzzles()

    def convert_chapters(self) -> None:
        for chapter in self.chapters:
            chapter_dir = self.output_dir / format_chapter_dir(
                chapter["title"], chapter["position"]
            )
            print(
                f"Converting chapter: '{chapter["file"].path}' -> "
                f"{(chapter_dir / 'chapter.properties').relative_to(self.output_dir)}"
            )

            chapter_dir.mkdir()
            self.chapter_dirs[chapter["chapterId"]] = chapter_dir

            (chapter_dir / "chapter.properties").write_text(
                f"""title={chapter["title"]}
description={chapter["description"]}
"""
            )

    def convert_puzzles(self) -> None:
        for puzzle in self.puzzles:
            chapter_dir = self.chapter_dirs[puzzle["chapterId"]]
            puzzle_dir = chapter_dir / format_puzzle_dir(
                puzzle["title"], puzzle["position"]
            )
            print(
                f"Converting puzzle: '{puzzle["file"].path}' -> "
                f"{(puzzle_dir / 'puzzle.properties').relative_to(self.output_dir)}"
            )

            puzzle_dir.mkdir()
            properties_str = f"""title={puzzle["title"]}
description={puzzle["description"]}
type={puzzle["activeRole"]}
gameLevel={puzzle["gameLevel"]}
"""
            if puzzle["activeRole"] == "ATTACKER" and puzzle["editableLinesStart"]:
                properties_str += f"editableLinesStart={puzzle["editableLinesStart"]}\n"
            if puzzle["activeRole"] == "ATTACKER" and puzzle["editableLinesEnd"]:
                properties_str += f"editableLinesEnd={puzzle["editableLinesEnd"]}\n"
            (puzzle_dir / "puzzle.properties").write_text(properties_str)

            cut_dir = puzzle_dir / "cut"
            cut_dir.mkdir()
            cut_file = self.cuts[puzzle["alias"]]
            (cut_dir / cut_file.path.name).write_text(cut_file.content)

            mutants_dir = puzzle_dir / "mutants"
            mutants_dir.mkdir()
            index = 1
            for mutant_position in [
                int(pos) for pos in str(puzzle["mutants"]).split(",")
            ]:
                mutant = self.mutants[(puzzle["alias"], mutant_position)]
                mutant_dir = mutants_dir / f"{index:02d}"
                index += 1
                mutant_dir.mkdir()
                (mutant_dir / mutant.path.name).write_text(mutant.content)

            tests_dir = puzzle_dir / "tests"
            tests_dir.mkdir()
            index = 1
            for test_position in [int(pos) for pos in str(puzzle["tests"]).split(",")]:
                test = self.tests[(puzzle["alias"], test_position)]
                test_dir = tests_dir / f"{index:02d}"
                index += 1
                test_dir.mkdir()
                (test_dir / test.path.name).write_text(test.content)


def main() -> None:
    if len(sys.argv) != 3:
        exit_with_usage()

    input_path = Path(sys.argv[1])
    input_files = []
    if input_path.is_dir():
        input_files = [
            TextFile(path.relative_to(input_path), path.read_text())
            for path in input_path.rglob("*")
            if path.is_file()
        ]
    elif input_path.is_file() and input_path.name.endswith(".zip"):
        input_files = read_zip(input_path)
    else:
        print("Invalid input path.", file=sys.stderr)
        exit_with_usage()

    output_dir = Path(sys.argv[2])
    if not output_dir.exists():
        output_dir.mkdir()
    if output_dir.is_file():
        print("Output path cannot be a file.", file=sys.stderr)
        exit_with_usage()

    PuzzleConverter(input_files, output_dir).convert()


if __name__ == "__main__":
    main()
