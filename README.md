Project Name: Wumpus World

Original source code: https://github.com/riyt/Wumpus_World_Student

Project Description: The Wumpus World is a cave consisting of rooms connected by passageways. The project requires us to implement a knowledge-based AI agent (MyAI.java), which is able to find gold in the Wumpus World and climb out of the cave. Please refer to "Student_Booklet.pdf" for more details.

Tournament: After submitting our code, we were entered into a tournament, which ran our agents across 10,000 worlds of variable sizes from 4x4 to 7x7. The ranking was based on the average score. Our team was "David8" and ranked 4th among 54 teams in this tournament. Please refer to "FinalAI tournament ranking.csv" for the list of the teams and their performances.

Project Report: Please refer to "project report.pdf"

How to compile and run in Linux:
Download the files in the "Wumpus_World_Java_Shell" folder into a folder.
In the terminal, change the directory to that folder.
Type "make" to compile the code.
And then navigate to the bin folder. You should find the compiled program inside. Typing "java -jar Wumpus_World.jar -d" will construct a random 4x4 world, run the MyAI agent on the world, and print output to the console. Please refer to "VI. Appendix: Shell Manual" in "Student_Booklet.pdf" for more running options.

Supplemental Information:
I tried two moving rules: Random Walk and A* Search. Surprisingly, the former performs better than the latter, so MyAI in the "Wumpus_World_Java_Shell" folder utilizes Random Walk to move around. You can also rename "MyAI_AStar.java" in the outer folder to "MyAI.java" and substitute the Random Walk version in the "Wumpus_World_Java_Shell" folder to try the A* Search version.
