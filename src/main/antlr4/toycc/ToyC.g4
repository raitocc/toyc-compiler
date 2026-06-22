grammar ToyC;

// ============================================================
// Parser Rules
// ============================================================

compUnit
    : (decl | funcDef)+ EOF
    ;

decl
    : constDecl
    | varDecl
    ;

constDecl
    : CONST INT ID ASSIGN expr SEMICOLON
    ;

varDecl
    : INT ID ASSIGN expr SEMICOLON
    ;

stmt
    : block                                   # blockStmt
    | SEMICOLON                               # emptyStmt
    | expr SEMICOLON                          # exprStmt
    | ID ASSIGN expr SEMICOLON                # assignStmt
    | decl                                    # declStmt
    | IF LPAREN expr RPAREN stmt (ELSE stmt)? # ifStmt
    | WHILE LPAREN expr RPAREN stmt           # whileStmt
    | BREAK SEMICOLON                         # breakStmt
    | CONTINUE SEMICOLON                      # continueStmt
    | RETURN expr? SEMICOLON                  # returnStmt
    ;

block
    : LBRACE stmt* RBRACE
    ;

funcDef
    : (INT | VOID) ID LPAREN (param (COMMA param)*)? RPAREN block
    ;

param
    : INT ID
    ;

expr
    : lOrExpr
    ;

lOrExpr
    : lAndExpr                # lOrBase
    | lOrExpr OR lAndExpr     # lOrBin
    ;

lAndExpr
    : relExpr                 # lAndBase
    | lAndExpr AND relExpr    # lAndBin
    ;

relExpr
    : addExpr                                  # relBase
    | relExpr op=(LT | GT | LE | GE | EQ | NE) addExpr  # relBin
    ;

addExpr
    : mulExpr                       # addBase
    | addExpr op=(PLUS | MINUS) mulExpr  # addBin
    ;

mulExpr
    : unaryExpr                            # mulBase
    | mulExpr op=(MUL | DIV | MOD) unaryExpr  # mulBin
    ;

unaryExpr
    : primaryExpr                    # unaryBase
    | op=(PLUS | MINUS | NOT) unaryExpr  # unaryOp
    ;

primaryExpr
    : NUMBER                                      # numberExpr
    | ID                                          # idExpr
    | LPAREN expr RPAREN                          # parenExpr
    | ID LPAREN (expr (COMMA expr)*)? RPAREN      # callExpr
    ;

// ============================================================
// Lexer Rules
// ============================================================

CONST    : 'const';
INT      : 'int';
VOID     : 'void';
IF       : 'if';
ELSE     : 'else';
WHILE    : 'while';
BREAK    : 'break';
CONTINUE : 'continue';
RETURN   : 'return';

PLUS     : '+';
MINUS    : '-';
MUL      : '*';
DIV      : '/';
MOD      : '%';
ASSIGN   : '=';
EQ       : '==';
NE       : '!=';
LT       : '<';
GT       : '>';
LE       : '<=';
GE       : '>=';
AND      : '&&';
OR       : '||';
NOT      : '!';

LPAREN   : '(';
RPAREN   : ')';
LBRACE   : '{';
RBRACE   : '}';
SEMICOLON: ';';
COMMA    : ',';

ID
    : [_A-Za-z][_A-Za-z0-9]*
    ;

NUMBER
    : '-'? ('0' | [1-9][0-9]*)
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;
