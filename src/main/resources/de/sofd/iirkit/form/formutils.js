function __fillForm(paramString) {
    var params = URI("?"+paramString).query(true);
    $('form[action="submit_ecrf"] input').each(function() {
        var input = $(this)[0];
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
            input.checked = (-1 != $.inArray(input.value, values));
            break;
        }
    });
    $('form[action="submit_ecrf"] select').each(function() {
        var select = $(this)[0];
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
            opt.selected = (-1 != $.inArray(opt.value, values));
        }
    });
}
