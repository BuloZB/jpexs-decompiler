/*
 *  Copyright (C) 2010-2015 JPEXS, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.jpexs.decompiler.flash.action.deobfuscation;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.abc.ABC;
import com.jpexs.decompiler.flash.abc.types.MethodBody;
import com.jpexs.decompiler.flash.action.Action;
import com.jpexs.decompiler.flash.action.ActionList;
import com.jpexs.decompiler.flash.action.ActionLocalData;
import com.jpexs.decompiler.flash.action.fastactionlist.ActionItem;
import com.jpexs.decompiler.flash.action.fastactionlist.FastActionList;
import com.jpexs.decompiler.flash.action.fastactionlist.FastActionListIterator;
import com.jpexs.decompiler.flash.action.swf4.ActionAdd;
import com.jpexs.decompiler.flash.action.swf4.ActionAnd;
import com.jpexs.decompiler.flash.action.swf4.ActionAsciiToChar;
import com.jpexs.decompiler.flash.action.swf4.ActionCharToAscii;
import com.jpexs.decompiler.flash.action.swf4.ActionDivide;
import com.jpexs.decompiler.flash.action.swf4.ActionEquals;
import com.jpexs.decompiler.flash.action.swf4.ActionGetTime;
import com.jpexs.decompiler.flash.action.swf4.ActionIf;
import com.jpexs.decompiler.flash.action.swf4.ActionJump;
import com.jpexs.decompiler.flash.action.swf4.ActionLess;
import com.jpexs.decompiler.flash.action.swf4.ActionMBAsciiToChar;
import com.jpexs.decompiler.flash.action.swf4.ActionMBStringLength;
import com.jpexs.decompiler.flash.action.swf4.ActionMultiply;
import com.jpexs.decompiler.flash.action.swf4.ActionNot;
import com.jpexs.decompiler.flash.action.swf4.ActionOr;
import com.jpexs.decompiler.flash.action.swf4.ActionPush;
import com.jpexs.decompiler.flash.action.swf4.ActionStringAdd;
import com.jpexs.decompiler.flash.action.swf4.ActionStringEquals;
import com.jpexs.decompiler.flash.action.swf4.ActionStringLength;
import com.jpexs.decompiler.flash.action.swf4.ActionStringLess;
import com.jpexs.decompiler.flash.action.swf4.ActionSubtract;
import com.jpexs.decompiler.flash.action.swf4.ActionToInteger;
import com.jpexs.decompiler.flash.action.swf4.ConstantIndex;
import com.jpexs.decompiler.flash.action.swf4.RegisterNumber;
import com.jpexs.decompiler.flash.action.swf5.ActionAdd2;
import com.jpexs.decompiler.flash.action.swf5.ActionBitAnd;
import com.jpexs.decompiler.flash.action.swf5.ActionBitLShift;
import com.jpexs.decompiler.flash.action.swf5.ActionBitOr;
import com.jpexs.decompiler.flash.action.swf5.ActionBitRShift;
import com.jpexs.decompiler.flash.action.swf5.ActionBitURShift;
import com.jpexs.decompiler.flash.action.swf5.ActionBitXor;
import com.jpexs.decompiler.flash.action.swf5.ActionDecrement;
import com.jpexs.decompiler.flash.action.swf5.ActionEquals2;
import com.jpexs.decompiler.flash.action.swf5.ActionIncrement;
import com.jpexs.decompiler.flash.action.swf5.ActionLess2;
import com.jpexs.decompiler.flash.action.swf5.ActionModulo;
import com.jpexs.decompiler.flash.action.swf5.ActionPushDuplicate;
import com.jpexs.decompiler.flash.action.swf5.ActionToNumber;
import com.jpexs.decompiler.flash.action.swf5.ActionToString;
import com.jpexs.decompiler.flash.action.swf5.ActionTypeOf;
import com.jpexs.decompiler.flash.action.swf6.ActionGreater;
import com.jpexs.decompiler.flash.action.swf6.ActionStringGreater;
import com.jpexs.decompiler.flash.ecma.EcmaScript;
import com.jpexs.decompiler.flash.helpers.SWFDecompilerListener;
import com.jpexs.decompiler.graph.Graph;
import com.jpexs.decompiler.graph.GraphTargetItem;
import com.jpexs.decompiler.graph.TranslateException;
import com.jpexs.decompiler.graph.TranslateStack;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JPEXS
 */
public class ActionDeobfuscatorSimpleFast implements SWFDecompilerListener {

    private final int executionLimit = 30000;

    @Override
    public void actionListParsed(ActionList actions, SWF swf) throws InterruptedException {
        FastActionList fastActions = new FastActionList(actions);
        fastActions.expandPushes();
        removeGetTimes(fastActions);
        //removeObfuscationIfs(fastActions);
        actions.setActions(fastActions.toActionList());
    }

    private boolean removeGetTimes(FastActionList actions) {
        if (actions.isEmpty()) {
            return false;
        }

        boolean changed = true;
        int getTimeCount = 1;
        while (changed && getTimeCount > 0) {
            changed = false;
            actions.removeUnreachableActions();
            actions.removeZeroJumps();
            getTimeCount = 0;

            // GetTime, If => Jump, assume GetTime > 0
            FastActionListIterator iterator = actions.iterator();
            while (iterator.hasNext()) {
                Action a = iterator.next().action;
                ActionItem a2Item = iterator.peek(0);
                Action a2 = a2Item.action;
                boolean isGetTime = a instanceof ActionGetTime;
                if (isGetTime) {
                    getTimeCount++;
                }

                if (isGetTime && a2 instanceof ActionIf) {
                    ActionJump jump = new ActionJump(0);
                    ActionItem jumpItem = new ActionItem(jump);
                    jumpItem.jumpTarget = a2Item.jumpTarget;
                    iterator.remove(); // GetTime
                    iterator.next();
                    iterator.remove(); // If
                    iterator.add(jumpItem); // replace If with Jump
                    changed = true;
                    getTimeCount--;
                    break;
                }
            }

            if (!changed && getTimeCount > 0) {
                // GetTime, Increment If => Jump
                iterator = actions.iterator();
                while (iterator.hasNext()) {
                    Action a = iterator.next().action;
                    Action a1 = iterator.peek(0).action;
                    ActionItem a2Item = iterator.peek(1);
                    Action a2 = a2Item.action;
                    if (a instanceof ActionGetTime && a1 instanceof ActionIncrement && a2 instanceof ActionIf) {
                        ActionJump jump = new ActionJump(0);
                        ActionItem jumpItem = new ActionItem(jump);
                        jumpItem.jumpTarget = a2Item.jumpTarget;
                        iterator.remove(); // GetTime
                        iterator.next();
                        iterator.remove(); // Increment
                        iterator.next();
                        iterator.remove(); // If
                        iterator.add(jumpItem); // replace If with Jump
                        changed = true;
                        break;
                    }
                }
            }
        }

        return false;
    }

//    private boolean removeObfuscationIfs(FastActionList actions) throws InterruptedException {
//        if (actions.isEmpty()) {
//            return false;
//        }
//
//        actions.removeUnreachableActions();
//        actions.removeZeroJumps();
//
//        FastActionListIterator iterator = actions.iterator();
//        while (iterator.hasNext()) {
//            ActionItem actionItem = iterator.next();
//            ExecutionResult result = new ExecutionResult();
//            executeActions(actions, i, actions.size() - 1, result);
//
//            if (result.idx != -1) {
//                int newIstructionCount = 1 /*jump */ + result.stack.size();
//                List<Action> unreachable = actions.getUnreachableActions(i, result.idx);
//                int unreachableCount = unreachable.size();
//
//                if (newIstructionCount < unreachableCount) {
//                    Action target = actions.get(result.idx);
//                    Action prevAction = actions.get(i);
//
//                    if (result.stack.isEmpty() && prevAction instanceof ActionJump) {
//                        ActionJump jump = (ActionJump) prevAction;
//                        jump.setJumpOffset((int) (target.getAddress() - jump.getAddress() - jump.getTotalActionLength()));
//                    } else {
//                        if (!result.stack.isEmpty()) {
//                            ActionPush push = new ActionPush(0);
//                            push.values.clear();
//                            for (GraphTargetItem graphTargetItem : result.stack) {
//                                push.values.add(graphTargetItem.getResult());
//                            }
//                            push.setAddress(prevAction.getAddress());
//                            actions.addAction(i++, push);
//                            prevAction = push;
//                        }
//
//                        ActionJump jump = new ActionJump(0);
//                        jump.setAddress(prevAction.getAddress());
//                        jump.setJumpOffset((int) (target.getAddress() - jump.getAddress() - jump.getTotalActionLength()));
//                        actions.addAction(i++, jump);
//                    }
//
//                    Action nextAction = actions.size() > i ? actions.get(i) : null;
//
//                    actions.removeUnreachableActions();
//                    actions.removeZeroJumps();
//
//                    if (nextAction != null) {
//                        int nextIdx = actions.indexOf(nextAction);
//                        if (nextIdx < i) {
//                            i = nextIdx;
//                        }
//                    }
//                }
//            }
//        }
//
//        return false;
//    }
    protected boolean isFakeName(String name) {
        for (char ch : name.toCharArray()) {
            if (ch > 31) {
                return false;
            }
        }

        return true;
    }

    private void executeActions(ActionList actions, int idx, int endIdx, ExecutionResult result) throws InterruptedException {
        List<GraphTargetItem> output = new ArrayList<>();
        ActionLocalData localData = new ActionLocalData();
        FixItemCounterTranslateStack stack = new FixItemCounterTranslateStack("");
        int instructionsProcessed = 0;

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            if (idx > endIdx) {
                break;
            }

            if (instructionsProcessed > executionLimit) {
                break;
            }

            Action action = actions.get(idx);

            /*System.out.print(action.getASMSource(actions, new ArrayList<Long>(), ScriptExportMode.PCODE));
             for (int j = 0; j < stack.size(); j++) {
             System.out.print(" '" + stack.get(j).getResult() + "'");
             }
             System.out.println();*/
            // do not throw EmptyStackException, much faster
            int requiredStackSize = action.getStackPopCount(localData, stack);
            if (stack.size() < requiredStackSize) {
                return;
            }

            action.translate(localData, stack, output, Graph.SOP_USE_STATIC, "");

            if (!(action instanceof ActionPush
                    || action instanceof ActionPushDuplicate
                    //|| action instanceof ActionPop
                    || action instanceof ActionAsciiToChar
                    || action instanceof ActionCharToAscii
                    || action instanceof ActionDecrement
                    || action instanceof ActionIncrement
                    || action instanceof ActionNot
                    || action instanceof ActionToInteger
                    || action instanceof ActionToNumber
                    || action instanceof ActionToString
                    || action instanceof ActionTypeOf
                    || action instanceof ActionStringLength
                    || action instanceof ActionMBAsciiToChar
                    || action instanceof ActionMBStringLength
                    || action instanceof ActionAnd
                    || action instanceof ActionAdd
                    || action instanceof ActionAdd2
                    || action instanceof ActionBitAnd
                    || action instanceof ActionBitLShift
                    || action instanceof ActionBitOr
                    || action instanceof ActionBitRShift
                    || action instanceof ActionBitURShift
                    || action instanceof ActionBitXor
                    || action instanceof ActionDivide
                    || action instanceof ActionEquals
                    || action instanceof ActionEquals2
                    || action instanceof ActionGreater
                    || action instanceof ActionLess
                    || action instanceof ActionLess2 // todo: fix (tz.swf/frame_6/DoAction: _loc3_.icon.gotoAndStop((Number(item.cost) || 0) >= 0?1:2)
                    || action instanceof ActionModulo
                    || action instanceof ActionMultiply
                    || action instanceof ActionOr
                    || action instanceof ActionStringAdd
                    || action instanceof ActionStringEquals
                    || action instanceof ActionStringGreater
                    || action instanceof ActionStringLess
                    || action instanceof ActionSubtract
                    || action instanceof ActionIf
                    || action instanceof ActionJump)) {
                break;
            }

            if (action instanceof ActionPush) {
                ActionPush push = (ActionPush) action;
                boolean ok = true;
                instructionsProcessed += push.values.size() - 1;
                for (Object value : push.values) {
                    if (value instanceof ConstantIndex || value instanceof RegisterNumber) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) {
                    break;
                }
            }

            idx++;

            if (action instanceof ActionJump) {
                ActionJump jump = (ActionJump) action;
                long address = jump.getTargetAddress();
                idx = actions.getIndexByAddress(address);
                if (idx == -1) {
                    throw new TranslateException("Jump target not found: " + address);
                }
            }

            if (action instanceof ActionIf) {
                ActionIf aif = (ActionIf) action;
                if (stack.isEmpty()) {
                    return;
                }

                if (EcmaScript.toBoolean(stack.pop().getResult())) {
                    long address = aif.getTargetAddress();
                    idx = actions.getIndexByAddress(address);
                    if (idx == -1) {
                        throw new TranslateException("If target not found: " + address);
                    }
                }
            }

            instructionsProcessed++;

            if (stack.allItemsFixed() && !(action instanceof ActionPush)) {
                result.idx = idx == actions.size() ? idx - 1 : idx;
                result.instructionsProcessed = instructionsProcessed;
                result.stack.clear();
                result.stack.addAll(stack);
            }
        }
    }

    @Override
    public byte[] proxyFileCatched(byte[] data) {
        return null;
    }

    @Override
    public void swfParsed(SWF swf) {
    }

    @Override
    public void abcParsed(ABC abc, SWF swf) {
    }

    @Override
    public void methodBodyParsed(MethodBody body, SWF swf) {
    }

    class ExecutionResult {

        public int idx = -1;

        public int instructionsProcessed = -1;

        public TranslateStack stack = new TranslateStack("?");

        public Object resultValue;
    }
}
