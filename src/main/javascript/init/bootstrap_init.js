/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
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
