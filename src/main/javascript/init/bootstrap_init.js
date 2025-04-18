import '../thirdparty/bootstrap';
import {Tooltip} from '../thirdparty/bootstrap';


/*
 * Display form validation through Bootstrap and disable form submissions if there are invalid fields.
 * To use this on a form add the 'needs-validation' class to it: <form class='needs-validation'>
 *
 * Adopted from https://getbootstrap.com/docs/5.0/forms/validation
 */
document.addEventListener('DOMContentLoaded', function() {
    for (const form of document.querySelectorAll('.needs-validation')) {
        // Disable default validation feedback.
        form.setAttribute('novalidate', '');

        form.addEventListener('submit', function (event) {
            if (!form.checkValidity()) {
                // Prevent submission when form is invalid.
                event.preventDefault();
                event.stopPropagation();
                // Display bootstrap's validation.
                form.classList.add('was-validated')
            }
        }, false);
    }
});


/*
 * Initialize tooltips.
 *
 * Adopted from https://getbootstrap.com/docs/5.0/components/tooltips
 */
document.addEventListener('DOMContentLoaded', function() {
    for (const tooltipTriggerEl of document.querySelectorAll('[data-bs-toggle="tooltip"]')) {
        new Tooltip(tooltipTriggerEl);
    }
});

/*
 * Initialize multi-level dropdown.
 *
 * Adopted from https://github.com/dallaslu/bootstrap-5-multi-level-dropdown
 */
document.addEventListener('DOMContentLoaded', function () {
    (function (bs) {
        const classname = 'has-child-dropdown-show';
        bs.Dropdown.prototype.toggle = function (original) {
            return function () {
                document.querySelectorAll(`.${classname}`).forEach(function (e) {
                    e.classList.remove(classname);
                });
                let dd = this._element.closest('.dropdown').parentNode.closest('.dropdown');
                for (; dd && dd !== document; dd = dd.parentNode.closest('.dropdown')) {
                    dd.classList.add(classname);
                }
                return original.call(this);
            }
        }(bs.Dropdown.prototype.toggle);

        document.querySelectorAll('.dropdown').forEach(function (dd) {
            dd.addEventListener('hide.bs.dropdown', function (e) {
                if (this.classList.contains(classname)) {
                    this.classList.remove(classname);
                    e.preventDefault();
                }
                e.stopPropagation(); // do not need pop in multi level mode
            });
        });
    })(bootstrap);
});
