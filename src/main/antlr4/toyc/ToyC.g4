grammar ToyC;

// Parser rules
compUnit : funcDef+ EOF ;

funcDef : type ID '(' ')' block ;

type : 'int' | 'void' ;

block : '{' stmt* '}' ;

stmt : 'return' expr ';' ;

expr : NUMBER ;

// Lexer rules
ID : [_A-Za-z][_A-Za-z0-9]* ;
NUMBER : '-'? ('0' | [1-9][0-9]*) ;

WS : [ \t\r\n]+ -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
