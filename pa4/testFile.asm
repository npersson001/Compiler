  0         PUSH         0
  1         LOADL        0
  2         CALL         newarr  
  3         CALL         L10
  4         HALT   (0)   
  5  L10:   PUSH         2
  6         LOADL        -1
  7         LOADL        0
  8         CALL         newobj  
  9         STORE        3[LB]
 10         LOADL        0
 11         STORE        4[LB]
 12         JUMP         L12
 13  L11:   PUSH         0
 14         LOAD         3[LB]
 15         CALLI        L13
 16         CALL         dispose 
 17         LOAD         4[LB]
 18         LOADL        1
 19         CALL         add     
 20         STORE        4[LB]
 21         POP          0
 22  L12:   LOAD         4[LB]
 23         LOADL        1025
 24         CALL         lt      
 25         JUMPIF (1)   L11
 26         LOADL        25
 27         CALL         putintnl
 28         RETURN (0)   1
 29  L13:   PUSH         0
 30         LOADL        55
 31         RETURN (1)   0
