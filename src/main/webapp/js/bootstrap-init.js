/*
 * Display form validation through Bootstrap and disable form submissions if there are invalid fields.
 * To use this on a form add the 'needs-validation' class to it: <form class='needs-validation'>
 *
 * Adopted from https://getbootstrap.com/docs/5.0/forms/validation
 */
$(document).ready(function() {
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
$(document).ready(function() {
    for (const tooltipTriggerEl of document.querySelectorAll('[data-bs-toggle="tooltip"]')) {
        new bootstrap.Tooltip(tooltipTriggerEl);
    }
});
