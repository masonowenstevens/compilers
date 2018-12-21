import java.util.List;

class Task3 {
    public static Codegen create () throws CodegenException { 
        return new Codegen(){
            public String exitLabel;
            public String loopName;
            public String codegen (Program p) throws CodegenException{
                List <Declaration> decls = p.decls;
                String program = "";
                for(int i = 0; i<decls.size(); i++){
                    Declaration temp = decls.get(i);
                    program += genDecl(temp);
                }
                return program;
            }            

            private String genExp (Exp e) throws CodegenException{
                String output = "";
                if(e instanceof IntLiteral){
                    IntLiteral x = (IntLiteral)e;
                    output += ("\tli $a0 " + x.n);
                }
                else if(e instanceof Variable){
                    Variable x = (Variable)e;
                    int offset = (4 * x.x);
                    output += ("\tlw $a0 " + offset + "($fp)");
                }
                else if(e instanceof Assign){
                    Assign x = (Assign)e;
                    int offset = (4 * x.x);
                    output += (genExp(x.e) + "\tsw $a0 " + offset + "($fp)");
                }
                else if(e instanceof If){
                    If x = (If)e;
                    String elseBranch = "else" + x.toString();
                    String thenBranch = "then" + x.toString();
                    exitLabel = "exit" + x.toString();
                    String i = "";
                    if(x.comp instanceof Equals){
                        i = "\tbeq $a0 $t1 ";
                    }
                    output += (comp(x) + thenBranch + "\n" +
                        elseBranch + ": \n" +
                        genExp(x.elseBody) +
                        "\tb " + exitLabel +"\n" +
                        thenBranch + ":\n" +
                        genExp(x.thenBody) +
                        exitLabel + ":\n");
                }
                else if(e instanceof Binexp){
                    Binexp x = (Binexp)e;
                    String i = "";
                    if(x.binop instanceof Plus){
                        i = "\tadd $a0 $t1 $a0\n";
                    }
                    else if(x.binop instanceof Minus){
                        i = "\tsub $a0 $t1 $a0\n";
                    }
                    else if(x.binop instanceof Times){
                        i = ("mult $a0 $t1\n" + "mflo $a0\n");
                    }
                    else if(x.binop instanceof Div){
                        i = ("div $a0 $t1\n" + "mflo $a0\n");
                    }
                    output +=   (genExp(x.l) +
                            "\tsw $a0 0($sp)\n" +
                            "\taddiu $sp $sp -4\n" +
                            genExp(x.r) +
                            "\tlw $t1 4($sp)\n" +
                            i + "\taddiu $sp $sp 4");
                }
                else if(e instanceof Invoke){
                    Invoke x = (Invoke)e;
                    List<Exp> args = x.args;
                    output += ("\tsw $fp 0($sp)\n" +
                        "\taddiu $sp $sp -4\n");
                    for(int i = args.size(); i > 0; i--){
                        output += (genExp(args.get(i-1))+
                                   "\tsw $a0 0($sp)\n" +
                                   "\taddiu $sp $sp -4\n");
                    }
                    output += "\tjal " + x.name +"_entry";
                }
                else if(e instanceof While){
                    While x = (While) e;
                    loopName = "loop" + x.toString();
                    exitLabel = "exit" + x.toString();
                    output +=  (loopName +": " + comp(x) +
                               genExp(x.body) + 
                               exitLabel +
                               "\tb " + loopName + "\n" 
                               + exitLabel + ":");
                    
                }
                else if(e instanceof RepeatUntil){
                    RepeatUntil x = (RepeatUntil) e;
                    loopName = "loop" + x.toString();
                    exitLabel = "exit" + x.toString();
                    output+=   (loopName +": " + genExp(x.body) +
                               comp(x) + exitLabel +
                               "b " + loopName + "\n" +
                               exitLabel + ":");
                }
                else if(e instanceof Assign){
                    Assign x = (Assign) e;
                    genExp(x);
                    output += "\tsw $a0 " + (x.x * 4) + " ($fp)";
                }
                else if(e instanceof Seq){
                    Seq x = (Seq) e;
                    genExp(x.l);
                    genExp(x.r);
                    output +=   ("\tsw $a0 0($sp)\n" +
                                "\taddiu $sp $sp -4\n");
                }
                else if(e instanceof Skip){
                    output += "\tnop\n";
                }
                else if(e instanceof Break){
                    output += ("\tjal " + exitLabel);
                }
                else if(e instanceof Continue){
                    output += ("\tjal " + loopName);
                }
                else{
                    throw new CodegenException("Invalid Expression " + e);
                }
                output += "\n";
                return output;
            }
            
            private String comp(Exp e) throws CodegenException{
                String i = "";
                Comp c = null;
                if(e instanceof If){
                    If x = (If) e;
                    i += (genExp(x.l) +
                          "\tsw $a0 0 ($sp)\n" +
                          "\taddiu $sp $sp -4\n" +
                          genExp(x.r)+
                          "\tlw $t1 4($sp)\n" +
                          "\taddiu $sp $sp 4\n");
                    c = x.comp;
                }
                else if(e instanceof While){
                    While x = (While) e;
                    i += (genExp(x.l) +
                          "\tsw $a0 0 ($sp)\n" +
                          "\taddiu $sp $sp -4\n" +
                          genExp(x.r)+
                          "\tlw $t1 4($sp)\n" +
                          "\taddiu $sp $sp 4\n");
                    c = x.comp;
                }
                else if(e instanceof RepeatUntil){
                    RepeatUntil x = (RepeatUntil) e;
                    i += (genExp(x.l) +
                          "\tsw $a0 0 ($sp)\n" +
                          "\taddiu $sp $sp -4\n" +
                          genExp(x.r)+
                          "\tlw $t1 4($sp)\n" +
                          "\taddiu $sp $sp 4\n");
                    c = x.comp;
                }
                if(c instanceof Equals){
                    i += "\tbeq $a0 $t1 ";
                }
                else if(c instanceof Less){
                    i += "\tblt $a0 $t1 ";
                }
                else if(c instanceof LessEq){
                    i += "\tble $a0 $t1 ";
                }
                else if(c instanceof Greater){
                    i += "\tbgt $a0 $t1 ";
                }
                else if(c instanceof GreaterEq){
                    i += "\tbge $a0 $t1 ";
                }
                return i;
            }
            
            private String genDecl(Declaration d) throws CodegenException{
                if(d.body instanceof Invoke){
                    int sizeAR = (2 + d.numOfArgs) * 4;
                    return (d.id + "_entry:\n" +
                               "\tmove $fp $sp\n" +
                               "\tsw $ra 0($sp)\n" +
                               "\taddiu $sp $sp -4\n" +
                               genExp(d.body) +
                               "\tlw $ra 4($sp)\n" +
                               "\taddiu $sp $sp " + sizeAR + "\n" +
                               "\tlw $fp 0($sp)\n" +
                               "\tjr $ra\n");
                }
                else{
                    return(d.id + "_entry:\n" +
                               "\tmove $fp $sp\n" +
                               "\tsw $ra 0($sp)\n" +
                               "\taddiu $sp $sp -4\n" +
                               genExp(d.body));
                }
            }
        };
    }
}
