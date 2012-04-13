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
            if (values.length > 0) {
                input.value = values[0];
            }
            break;

        case 'checkbox':
        case 'radio':
            input.checked = (-1 != $.inArray(input.value, values));
            break;
        }
    });
}
