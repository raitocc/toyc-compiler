grammar ToyC;



// ---- Parser Rules ----
compUnit : (decl | funcDef)+ EOF ;

decl : constDecl | varDecl ;

constDecl : 'const' 'int' ID '=' expr ';' ;

varDecl : 'int' ID '=' expr ';' ;

stmt : block
     | ';'
     | expr ';'
     | ID '=' expr ';'
     | decl
     | 'if' '(' expr ')' stmt ('else' stmt)?
     | 'while' '(' expr ')' stmt
     | 'break' ';'
     | 'continue' ';'
     | 'return' expr? ';'
     ;

block : '{' stmt* '}' ;

funcDef : ('int' | 'void') ID '(' (param (',' param)*)? ')' block ;

param : 'int' ID ;

expr : primaryExpr                                                     # ExprPrimary
     | ('+' | '-' | '!') expr                                          # ExprUnary
     | expr op=('*' | '/' | '%') expr                                  # ExprMul
     | expr op=('+' | '-') expr                                        # ExprAdd
     | expr op=('<' | '>' | '<=' | '>=' | '==' | '!=') expr            # ExprRel
     | expr '&&' expr                                                  # ExprLAnd
     | expr '||' expr                                                  # ExprLOr
     ;

primaryExpr : ID                                                       # PrimaryId
            | NUMBER                                                   # PrimaryNumber
            | '(' expr ')'                                             # PrimaryParen
            | ID '(' (expr (',' expr)*)? ')'                           # PrimaryCall
            ;

// ---- Lexer Rules ----

// Identifiers
ID : [_A-Za-z][_A-Za-z0-9]* ;

// Decimal integers
// Support negative numbers directly in the lexer? 
// The grammar says NUMBER -> '-?'(0|[1-9][0-9]*)
// Wait! In the requirements: `NUMBER` -> `-?(0|[1-9][0-9]*)`
NUMBER : '-'? ('0' | [1-9][0-9]*) ;

// Whitespace and BOM
WS : [ \t\r\n\uFEFF]+ -> skip ;

// Comments
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
