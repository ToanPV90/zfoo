/*
 * Copyright (C) 2020 The zfoo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.zfoo.protocol.serializer.golang;

import com.zfoo.protocol.generate.GenerateProtocolFile;
import com.zfoo.protocol.registration.field.IFieldRegistration;
import com.zfoo.protocol.registration.field.SetField;
import com.zfoo.protocol.serializer.CodeLanguage;
import com.zfoo.protocol.serializer.CutDownSetSerializer;
import com.zfoo.protocol.util.StringUtils;

import java.lang.reflect.Field;

import static com.zfoo.protocol.util.FileUtils.LS;

/**
 * @author godotg
 */
public class GoSetSerializer implements IGoSerializer {

    @Override
    public String fieldType(Field field, IFieldRegistration fieldRegistration) {
        var setField = (SetField) fieldRegistration;
        var registration = setField.getSetElementRegistration();
        var type = CodeGenerateGolang.goSerializer(registration.serializer()).fieldType(field, registration);
        return StringUtils.format("[]{}", type);
    }

    @Override
    public void writeObject(StringBuilder builder, String objectStr, int deep, Field field, IFieldRegistration fieldRegistration) {
        GenerateProtocolFile.addTab(builder, deep);
        if (CutDownSetSerializer.getInstance().writeObject(builder, objectStr, field, fieldRegistration, CodeLanguage.Golang)) {
            return;
        }

        SetField setField = (SetField) fieldRegistration;

        builder.append(StringUtils.format("if {} == nil {", objectStr)).append(LS);
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append("buffer.WriteInt(0)").append(LS);

        GenerateProtocolFile.addTab(builder, deep);
        builder.append("} else {").append(LS);

        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append(StringUtils.format("buffer.WriteInt(len({}))", objectStr)).append(LS);

        GenerateProtocolFile.addTab(builder, deep + 1);
        String length = "length" + GenerateProtocolFile.localVariableId++;
        builder.append(StringUtils.format("var {} = len({})", length, objectStr)).append(LS);

        GenerateProtocolFile.addTab(builder, deep + 1);
        String i = "i" + GenerateProtocolFile.localVariableId++;
        builder.append(StringUtils.format("for {} := 0; {} < {}; {}++ {", i, i, length, i)).append(LS);
        GenerateProtocolFile.addTab(builder, deep + 2);
        String element = "element" + GenerateProtocolFile.localVariableId++;
        builder.append(StringUtils.format("var {} = {}[{}]", element, objectStr, i)).append(LS);

        CodeGenerateGolang.goSerializer(setField.getSetElementRegistration().serializer())
                .writeObject(builder, element, deep + 2, field, setField.getSetElementRegistration());

        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append("}").append(LS);
        GenerateProtocolFile.addTab(builder, deep);
        builder.append("}").append(LS);
    }

    @Override
    public String readObject(StringBuilder builder, int deep, Field field, IFieldRegistration fieldRegistration) {
        GenerateProtocolFile.addTab(builder, deep);
        var cutDown = CutDownSetSerializer.getInstance().readObject(builder, field, fieldRegistration, CodeLanguage.Golang);
        if (cutDown != null) {
            return cutDown;
        }

        SetField setField = (SetField) fieldRegistration;
        var result = "result" + GenerateProtocolFile.localVariableId++;

        var typeName = fieldType(field, fieldRegistration);

        var i = "index" + GenerateProtocolFile.localVariableId++;
        var size = "size" + GenerateProtocolFile.localVariableId++;

        builder.append(StringUtils.format("var {} = buffer.ReadInt()", size)).append(LS);
        GenerateProtocolFile.addTab(builder, deep);
        builder.append(StringUtils.format("var {} = make({}, {})", result, typeName, size)).append(LS);

        GenerateProtocolFile.addTab(builder, deep);
        builder.append(StringUtils.format("if {} > 0 {", size)).append(LS);

        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append(StringUtils.format("for {} := 0; {} < {}; {}++ {", i, i, size, i)).append(LS);
        var readObject = CodeGenerateGolang.goSerializer(setField.getSetElementRegistration().serializer())
                .readObject(builder, deep + 2, field, setField.getSetElementRegistration());
        GenerateProtocolFile.addTab(builder, deep + 2);
        builder.append(StringUtils.format("{}[{}] = {}", result, i, readObject)).append(LS);
        GenerateProtocolFile.addTab(builder, deep + 1);
        builder.append("}").append(LS);
        GenerateProtocolFile.addTab(builder, deep);
        builder.append("}").append(LS);

        return result;
    }

}
