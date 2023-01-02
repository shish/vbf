Shish's Visual BrainF\*ck
=========================

Intro
-----
Yo. Booyah. Etc. Index.

```
1) The GUI
     |-> The Vis Panel
     \-> The Bits under the Vis Panel
2) The Guide
     |-> BF History & Intro
     \-> BF++ Intro
3) The Future
     |-> Fix FIXMEs
     \-> Put BF on a chip
```

tl;dr:
```
javac vbf.java
java vbf
```


The GUI
-------

The Visualisation Panel
-----------------------
Dragging the main panel will drag it as though you have grabbed
the blackness, you can go anywhere, thus you have more control
but it takes longer to move linearly along the array. Now you
can't (shouldn't) get lost, as while you move there's a compass
line poining from the center of the screen to the start of the
disk.

Dragging the scrollbar will act as though you are dragging the
viewing portal across the black, it's more limited but you can't
get lost on it. It resets the black to a stable position to make
sure you are always looking at a sensible area.

I don't know why having the drag actions for main panel and
scrollbar being opposite works, but it feels nicer.

Bits under the Vis Panel
------------------------
A) A text field for editing the selected value (hit return to enter!)
B) Move selection back
C) Move selection forth



Quick Guide to BF
-----------------
BF is a Turing Complete (whatever that is) programming language
that's simple to learn but nearly impossible to make use of. It
has these commands:

- `<` move head left
- `>` move head right
- `+` increment value at head
- `-` decrement value at head
- `,` get a charachter from STDIN and place it in the head's location
- `.` print the ASCII value of the charachter under the head
- `[` start of loop
- `]` if value under head = 0, carry on, else go back to start of loop


Quick Guide to BF++
-------------------
When I first saw the original in action I thought it was so
beautiful I cried, the only other thing to have this effect was
el-vis's Winamp AVSes, now *they're* beautiful.

However I soon found BF limited in that you can't use it for
anything other than maths. I figured that it would be cool to
use it as a general language, even if that meant losing some of
the beauty.

BF++ is my addition to make BF more useful as a general purpose
language, it adds these commands:

- `v` move head down
- `^` move head up
- `i` read file into current row
- `o` write file out of current row
- `j` jump head to location on disk
- `;` to end of line is a comment
- `#` same as `;`

files are read like so:
the filename is put into a row, starting at index 0, "read" is called and the
file named from index 0 to the first 0x00 is read into the row, from 0 to the
end:

Disk view:
```
1938753945731945731498 - a row
hello.txt0731945731498 - the file name with 0 at the end
this is the hello.txt0 - the file
```


The Future
----------
A) Fix all the sections marked FIXME
Should be easy enough - there aren't any bugs as such, just bits
that I haven't got round to doing yet.

B) DIY a BF chip/circuit board (Buttons! LEDs! Yay!)
Buttons, transistors & LEDs should be easy enough to find

BF-CHIP to ASM translator:
- `+` = INC (memory)
- `-` = DEC
- `<` = INC (register)
- `>` = DEC
- `,` = Read the current pattern of button presses
- `.` = Flash of LEDs
- `[` = store location in register
- `]` = jump to stored location

Where the buttons and LEDs are bitmasks:
```
o=button / x=pressed
.=LED / *=lit

oxox <- buttons (5)
,+.  <- program to add 1
.**. <- LEDs (6)
```
