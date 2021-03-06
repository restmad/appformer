/*
 * Copyright (C) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.kie.appformer.flow.lang;

import static org.kie.appformer.flow.lang.AST.mapping;
import static org.kie.appformer.flow.lang.Token.constant;
import static org.kie.appformer.flow.lang.Token.identifier;
import static org.kie.appformer.flow.lang.Token.keyword;
import static org.kie.appformer.flow.lang.Token.operator;
import static org.kie.appformer.flow.lang.Token.symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.kie.appformer.flow.lang.AST.Assignment;
import org.kie.appformer.flow.lang.AST.ConfigExpression;
import org.kie.appformer.flow.lang.AST.Empty;
import org.kie.appformer.flow.lang.AST.Expression;
import org.kie.appformer.flow.lang.AST.Identifier;
import org.kie.appformer.flow.lang.AST.MapExpression;
import org.kie.appformer.flow.lang.AST.MatchableExpression;
import org.kie.appformer.flow.lang.AST.SimpleExpression;
import org.kie.appformer.flow.lang.AST.Statement;
import org.kie.appformer.flow.lang.AST.Type;

/**
 * <p>
 * Parses flow language source into an {@link AST}.
 *
 * <p>
 * <b>Note:</b> This implementation is basic and not very robust (for example, tokens are
 * generated by splitting on whitespace!). In the future this will either need to be enhanced or
 * replaced with a third-party parsing library.
 */
public class Parser {

    private static final Map<String, Token> KEYWORDS = new HashMap<>();
    private static final Map<String, Token> SYMBOLS = new HashMap<>();
    private static final Map<String, Token> OPERATORS = new HashMap<>();

    static {
        KEYWORDS.put( "export", keyword( "export" ) );
        KEYWORDS.put( "import", keyword( "import" ) );
        KEYWORDS.put( "as", keyword( "as" ) );
        KEYWORDS.put( "type", keyword( "type" ) );
        KEYWORDS.put( "true", keyword( "true" ) );
        KEYWORDS.put( "false", keyword( "false" ) );
        KEYWORDS.put( "default", keyword( "default" ) );

        SYMBOLS.put( ";", symbol( ";" ) );
        SYMBOLS.put( ",", symbol( "," ) );
        SYMBOLS.put( ":", symbol( ":" ) );
        SYMBOLS.put( "{", symbol( "{" ) );
        SYMBOLS.put( "}", symbol( "}" ) );
        SYMBOLS.put( "(", symbol( "(" ) );
        SYMBOLS.put( ")", symbol( ")" ) );
        SYMBOLS.put( "<", symbol( "<" ) );
        SYMBOLS.put( ">", symbol( ">" ) );

        OPERATORS.put( "->", operator( "->" ) );
        OPERATORS.put( "=", operator( "=" ) );
    }

    /**
     * <p>
     * Parses flow language source into a list of statements (effectively an {@link AST}).
     */
    public List<Statement> parse( final String source ) {
        final List<Token> tokens = tokenize(source);
        final List<Statement> stmts = parseStatements(tokens);
        return stmts;
    }

    private List<Statement> parseStatements( final List<Token> tokens ) {
        final List<Statement> stmts = new ArrayList<>();
        final Token stmtEndSymbol = SYMBOLS.get( ";" );

        int index = 0;
        while ( index < tokens.size() ) {
            final int stmtEnd = index + tokens.subList( index, tokens.size() ).indexOf( stmtEndSymbol );
            final List<Token> curStmt = tokens.subList( index, stmtEnd );
            final Statement stmt = parseStatement( curStmt );
            stmts.add( stmt );
            index = stmtEnd + 1;
        }

        return stmts;
    }

    private Statement parseStatement( final List<Token> curStmt ) {
        if ( curStmt.isEmpty() ) {
            return Empty.INSTANCE;
        }
        else {
            final Token first = curStmt.get( 0 );
            if ( first.isKeyword( "import" ) ) {
                assert curStmt.size() > 2;
                return parseImportStmt( curStmt );
            }
            else if ( first.isKeyword( "export" ) ) {
                return parseExportFlowStmt( curStmt );
            }
            else if ( first.isIdentifier() ) {
                return parseAssignmentStmt( curStmt );
            }
            else {
                throw new IllegalArgumentException( "Expected identifier for assignment, but found [" + first
                                                    + "] at start of statement [" + curStmt + "]." );
            }
        }
    }

    private Statement parseExportFlowStmt( final List<Token> curStmt ) {
        assert curStmt.size() > 1;
        final Assignment assignment = parseAssignmentStmt( curStmt.subList( 1, curStmt.size() ) );
        return new AST.ExportFlow( assignment );
    }

    private Assignment parseAssignmentStmt( final List<Token> assignmentTokens ) {
        final Identifier assignedId;
        final Optional<Type> assignedType;
        final Expression assignmentValue;

        int index = 0;

        validateIndex( assignmentTokens, index );
        if ( assignmentTokens.get( index ).isIdentifier() ) {
            assignedId = new AST.Identifier( assignmentTokens.get( 0 ).symbol );
            index += 1;
        }
        else {
            throw new IllegalArgumentException( "Expected identifier for assignment, but found ["
                                                + assignmentTokens.get( index )
                                                + "] at start of statement [" + assignmentTokens + "]." );
        }

        validateIndex( assignmentTokens, index );
        if ( assignmentTokens.get( index ).isSymbol( ":" ) ) {
            index += 1;
            if ( assignmentTokens.get( index ).isIdentifier() ) {
                final Identifier typeId = new AST.Identifier( assignmentTokens.get( index ).symbol );
                index += 1;
                if ( assignmentTokens.get( index ).isOperator( "->" ) ) {
                    index += 1;
                    if ( assignmentTokens.get( index ).isIdentifier() ) {
                        final Identifier otherTypeId = new AST.Identifier( assignmentTokens.get( index ).symbol );
                        assignedType = Optional.of( new AST.FlowType( typeId, otherTypeId ) );
                        index += 1;
                    }
                    else {
                        throw new IllegalArgumentException( "Expected an identifier for an output type after [->] but found ["
                                                            + assignmentTokens.get( index ) + "]." );
                    }
                }
                else {
                    assignedType = Optional.of( new AST.SimpleType( typeId ) );
                    index += 1;
                }
            }
            else {
                throw new IllegalArgumentException( "Expected identifier for a type after [:] but found ["
                                                    + assignmentTokens.get( index ) + "]." );
            }
        }
        else {
            assignedType = Optional.empty();
        }

        if ( assignmentTokens.get( index ).isOperator( "=" ) ) {
            index += 1;
        }
        else {
            throw new IllegalArgumentException( "Expected assignment operator [=] but found ["
                                                + assignmentTokens.get( index ) + "]." );
        }

        validateIndex( assignmentTokens, index );
        assignmentValue = parseFlowExpression( assignmentTokens, index, o -> false ).result;

        return new AST.Assignment( assignedId, assignedType, assignmentValue );
    }

    private void validateIndex( final List<Token> tokens, final int index ) {
        if ( index >= tokens.size() ) {
            final String lastToken = ( tokens.size() > 0 ? tokens.get( tokens.size()-1 ).toString() : "" );
            throw new IllegalArgumentException( "Expected more tokens after [" + lastToken + "] but found none." );
        }
    }

    private ParseResult<Expression> parseFlowExpression( final List<Token> flowTokens, int index, final Predicate<Token> terminationCondition ) {
        final List<Expression> flowParts = new ArrayList<>();

        while ( index < flowTokens.size() && !terminationCondition.test( flowTokens.get( index ) ) ) {
            if ( flowTokens.get( index ).isOperator( "->" ) ) {
                index += 1;
                if ( index >= flowTokens.size() ) {
                    throw new IllegalArgumentException( "The [->] operator appearing after ["
                                                        + flowTokens.get( index - 1 )
                                                        + "] must be followed by a flow expression, but none was found." );
                }
            }
            else if ( flowTokens.get( index ).isSymbol( "{" ) ) {
                final ParseResult<MapExpression> parseRes = parseMapExpression( flowTokens, index );
                index = parseRes.index;
                flowParts.add( parseRes.result );
            }
            else if ( flowTokens.get( index ).isIdentifier() ) {
                final Optional<ParseResult<ConfigExpression>> oconfig = maybeParseConfigExpression( flowTokens, index );
                if ( oconfig.isPresent() ) {
                    flowParts.add( oconfig.get().result );
                    index = oconfig.get().index;
                }
                else {
                    flowParts.add( new AST.Identifier( flowTokens.get( index++ ).symbol ) );
                }
            }
            else if ( flowTokens.get( index ).isLiteral() ) {
                flowParts.add( new AST.Literal( flowTokens.get( index++ ).symbol ) );
            }
            else {
                throw new IllegalArgumentException( "Expected [->] or a flow expression but found ["
                                                    + flowTokens.get( index ) + "]." );
            }
        }

        final Expression result = ( flowParts.size() == 1 ? flowParts.get( 0 ) : new AST.FlowExpression( flowParts ) );
        return new ParseResult<>( result, index );
    }

    private ParseResult<MapExpression> parseMapExpression( final List<Token> flowTokens, int index ) {
        validateIndex( flowTokens, index );
        if ( !flowTokens.get( index ).isSymbol( "{" ) ) {
            throw new IllegalArgumentException( "Expected the start of a map expression [{] but found ["
                                                + flowTokens.get( index ) + "]." );
        }
        final LinkedHashMap<MatchableExpression, Expression> mappings = new LinkedHashMap<>();

        while ( !flowTokens.get( index ).isSymbol( "}" ) ) {
            index += 1;
            MatchableExpression key;
            final ParseResult<MatchableExpression> keyRes = parseMapExpressionKey( flowTokens, index );
            index = keyRes.index + 1;
            key = keyRes.result;
            final ParseResult<Expression> valueRes = parseFlowExpression( flowTokens, index, token -> token.isSymbol( "," ) || token.isSymbol( "}" ) );
            index = valueRes.index;
            final Expression value = valueRes.result;
            mappings.put( key, value );
        }

        return new ParseResult<>( mapping( mappings ), ++index );
    }

    private ParseResult<MatchableExpression> parseMapExpressionKey( final List<Token> flowTokens, int index ) {
        final Token first = flowTokens.get( index++ );
        if ( first.isLiteral() ) {
            return new ParseResult<>( new AST.Literal( first.symbol ), index );
        }
        else if ( first.isIdentifier() ) {
            final Token second = flowTokens.get( index++ );
            if ( second.isSymbol( "(" ) ) {
                final List<MatchableExpression> args = new ArrayList<>();
                index -= 1;
                while ( !flowTokens.get( index ).isSymbol( ")" ) ) {
                    final Token symbol = flowTokens.get( index++ );
                    if ( !symbol.isSymbol() ) {
                        throw new IllegalArgumentException();
                    }
                    final ParseResult<MatchableExpression> subKeyRes = parseMapExpressionKey( flowTokens, index );
                    index = subKeyRes.index;
                    args.add( subKeyRes.result );
                }
                return new ParseResult<>( new AST.ConstructorPattern( new AST.Identifier( first.symbol ), args ), ++index );
            }
            else {
                return new ParseResult<>( new AST.Identifier( first.symbol ), --index );
            }
        }
        else {
            throw new IllegalArgumentException( "Expected a literal or identifier to start a pattern-matching expression, but found ["
                                                + first + "]." );
        }
    }

    private Optional<ParseResult<ConfigExpression>> maybeParseConfigExpression( final List<Token> flowTokens,
                                                                                int index ) {
        if ( index < flowTokens.size() ) {
            if ( flowTokens.get( index ).isIdentifier() ) {
                final Identifier configId = new AST.Identifier( flowTokens.get( index++ ).symbol );
                final Map<Identifier, SimpleExpression> configMap = new LinkedHashMap<>();
                if ( index < flowTokens.size() && flowTokens.get( index ).isSymbol( "(" ) ) {
                    index += 1;
                    do {
                        if ( index+2 < flowTokens.size() ) {
                            final Token id = flowTokens.get( index++ );
                            final Token equals = flowTokens.get( index++ );
                            final Token value = flowTokens.get( index++ );

                            if ( id.isIdentifier() && equals.isOperator( "=" ) && ( value.isIdentifier() || value.isLiteral() ) ) {
                                final SimpleExpression valueExp = (value.isIdentifier() ? new AST.Identifier( value.symbol ) : new AST.Literal( value.symbol ));
                                configMap.put( new AST.Identifier( id.symbol ), valueExp );

                                validateIndex( flowTokens, index );
                                final Token symbol = flowTokens.get( index++ );
                                if ( symbol.isSymbol( "," ) ) {
                                    continue;
                                }
                                else if ( symbol.isSymbol( ")" ) ) {
                                    break;
                                }
                                else {
                                    throw new IllegalArgumentException( "Expected a property separator [,] or terminator [)] but found ["
                                                                        + symbol + "]." );
                                }
                            }
                            else {
                                if ( !id.isIdentifier() ) {
                                    throw new IllegalArgumentException( "Expected identifier for config property but found ["
                                                                        + id + "]." );
                                }
                                else if ( !equals.isIdentifier() ) {
                                    throw new IllegalArgumentException( "Expected assignment operator [=] for assigning to config property but found ["
                                                                        + equals + "]." );
                                }
                                else {
                                    throw new IllegalArgumentException( "Expected an identifier or literal as the value of a config property, but found ["
                                                                        + value + "]." );
                                }
                            }
                        }
                        else if ( index < flowTokens.size() && flowTokens.get( index ).isSymbol( ")" ) ) {
                            break;
                        }
                        else {
                            if ( index >= flowTokens.size() ) {
                                final String lastToken = (flowTokens.size() > 0 ? flowTokens.get( flowTokens.size()
                                                                                            - 1 ).toString() : "");
                                throw new IllegalArgumentException( "Expected config expression to continue but ran out of tokens after ["
                                                                    + lastToken + "]." );
                            }
                            else {
                                throw new IllegalArgumentException( "Not enough tokens to parse another config property assignment, but found ["
                                                                    + flowTokens.get( index )
                                                                    + "] instead of terminator [)]." );
                            }
                        }
                    } while ( true );

                    return Optional.of( new ParseResult<>( new AST.ConfigExpression( configId, configMap ), index ) );
                }
            }
        }

        return Optional.empty();
    }

    private Statement parseImportStmt( final List<Token> curStmt ) {
        assert curStmt.size() == 6 || curStmt.size() == 4;
        assert curStmt.get( 2 ).isSymbol( ":" );
        assert curStmt.size() == 4 || curStmt.get( 4 ).isOperator( "->" );
        final Identifier flowId = new AST.Identifier( curStmt.get( 1 ).symbol );
        final Type type;
        if ( curStmt.size() == 4 ) {
            type = new AST.SimpleType( new AST.Identifier( curStmt.get( 3 ).symbol ) );
        }
        else {
            type = new AST.FlowType( new AST.Identifier( curStmt.get( 3 ).symbol ),
                                                        new AST.Identifier( curStmt.get( 5 ).symbol ) );
        }
        return new AST.ImportIdentifier( flowId, type );
    }

    private List<Token> tokenize( final String source ) {
        // TODO support tokens without spaces between them
        final String[] symbols = source.split( "\\s+" );
        final Map<String, Token> identifiers = new HashMap<>();
        final Map<String, Token> constants = new HashMap<>();
        final List<Token> parsedTokens = new ArrayList<>(symbols.length);

        for (final String symbol : symbols) {
            if ( "".equals( symbol ) ) continue;
            Token token;
            if ((token = KEYWORDS.get( symbol )) != null
                    || (token = SYMBOLS.get( symbol )) != null
                    || (token = OPERATORS.get( symbol )) != null
                    || (token = identifiers.get( symbol )) != null
                    || (token = constants.get( symbol )) != null) {
            }
            else if ( isNumberLiteral( symbol ) ) {
                token = constant( symbol );
            }
            else {
                token = identifier( symbol );
            }
            parsedTokens.add( token );
        }

        return parsedTokens;
    }

    private boolean isNumberLiteral( final String symbol ) {
        boolean decimal = false;
        for (int i = 0; i < symbol.length(); i++) {
            final char cur = symbol.charAt( i );
            if ( cur == '.' ) {
                if ( decimal ) {
                    return false;
                }
                else {
                    decimal = true;
                }
            }
            else if ( !Character.isDigit( cur ) ) {
                return false;
            }
        }

        return true;
    }

    static class ParseResult<T> {
        final T result;
        final int index;

        public ParseResult( final T result, final int index ) {
            this.result = result;
            this.index = index;
        }
    }

}
