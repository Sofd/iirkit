function __fillForm(paramString) {
    if (typeof __userFillForm == 'function') {
        __userFillForm(paramString);
        return;
    }
    var params = URI("?"+paramString).query(true);
    $__iirkit_jquery('form[action="submit_ecrf"] input').each(function() {
        var input = $__iirkit_jquery(this)[0];
        var name = input.name;
        var values = params[name];
        switch (typeof(values)) {
        case 'string':
            values = [values];
            break;
        case 'undefined':
            values = [];
            break;
        }
        switch (input.type) {
        case 'text':
            input.value = (values.length > 0 ? values[0] : "");
            break;

        case 'checkbox':
        case 'radio':
            input.checked = (-1 != $__iirkit_jquery.inArray(input.value, values));
            break;
        }
    });
    $__iirkit_jquery('form[action="submit_ecrf"] select').each(function() {
        var select = $__iirkit_jquery(this)[0];
        var name = select.name;
        var values = params[name];
        switch (typeof(values)) {
        case 'string':
            values = [values];
            break;
        case 'undefined':
            values = [];
            break;
        }
        for (var i = 0; i < select.options.length; i++) {
            var opt = select.options[i];
            opt.selected = (-1 != $__iirkit_jquery.inArray(opt.value, values));
        }
    });
}

function __enableForm(enabled) {
    if (typeof __userEnableForm == 'function') {
        __userEnableForm(enabled);
        return;
    }
    $__iirkit_jquery('form[action="submit_ecrf"] input,select').each(function() {
        if (!('submit' == $__iirkit_jquery(this).prop('type'))) {
            $__iirkit_jquery(this).prop('disabled', !enabled);
        }
    });
}
