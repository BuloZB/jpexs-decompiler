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
package com.jpexs.decompiler.flash.abc.avm2.model;

import com.jpexs.decompiler.flash.abc.avm2.instructions.AVM2Instruction;
import com.jpexs.decompiler.flash.abc.avm2.model.clauses.AssignmentAVM2Item;
import com.jpexs.decompiler.flash.abc.types.Multiname;
import com.jpexs.decompiler.flash.helpers.GraphTextWriter;
import com.jpexs.decompiler.graph.GraphPart;
import com.jpexs.decompiler.graph.GraphTargetItem;
import com.jpexs.decompiler.graph.TypeItem;
import com.jpexs.decompiler.graph.model.LocalData;

public class SetSlotAVM2Item extends AVM2Item implements SetTypeAVM2Item, AssignmentAVM2Item {

    public Multiname slotName;

    //public GraphTargetItem value;
    public GraphTargetItem scope;

    public SetSlotAVM2Item(AVM2Instruction instruction, GraphTargetItem scope, Multiname slotName, GraphTargetItem value) {
        super(instruction, PRECEDENCE_ASSIGMENT, value);
        this.slotName = slotName;
        this.scope = scope;
    }

    @Override
    public GraphPart getFirstPart() {
        return value.getFirstPart();
    }

    @Override
    public GraphTextWriter appendTo(GraphTextWriter writer, LocalData localData) throws InterruptedException {
        getSrcData().localName = slotName == null ? "/*UnknownSlot*/" : slotName.getName(localData.constantsAvm2, localData.fullyQualifiedNames, false);
        getName(writer, localData);
        writer.append(" = ");
        return value.toString(writer, localData);
    }

    public String getNameAsStr(LocalData localData) {
        return slotName == null ? "/*UnknownSlot*/" : slotName.getName(localData.constantsAvm2, localData.fullyQualifiedNames, false);
    }

    public GraphTextWriter getName(GraphTextWriter writer, LocalData localData) {
        if (slotName == null) {
            return writer.append("/*UnknownSlot*/");
        }
        return writer.append(slotName.getName(localData.constantsAvm2, localData.fullyQualifiedNames, false));
    }

    @Override
    public GraphTargetItem getObject() {
        return new GetSlotAVM2Item(getInstruction(), scope, slotName);
    }

    @Override
    public GraphTargetItem getValue() {
        return value;
    }

    @Override
    public boolean hasSideEffect() {
        return true;
    }

    @Override
    public GraphTargetItem returnType() {
        return TypeItem.UNBOUNDED;
    }

    @Override
    public boolean hasReturnValue() {
        return false;
    }
}
