<div class="progress" id="progress" style="height: 40px; font-size: 30px; margin: 5px; display: none;">
    <div class="progress-bar" role="progressbar"
         style="font-size: 15px; line-height: 40px; width: 0;
                <%-- Disable animations because animations don't have time to finish before the page reloads. --%>
                transition: none; -o-transition: none; -webkit-transition: none;"
         aria-valuemin="0"
         aria-valuemax="100"
         aria-valuenow="0">
    </div>
</div>

<script>
    (function () {

        let progress = null;
        let progressBar = null;

        /**
         * Sets the progress of the progress bar.
         * @param {number} value The new width of the progress bar, as a number between 0 and 100.
         * @param {string} text The text to display on the progress bar.
         */
         window.setProgress = function (value, text) {
            if (progress === null) {
                progress = document.getElementById("progress");
                progressBar = progress.children[0];
            }

            progress.style.display = null;
            progressBar.setAttribute('aria-valuenow', value);
            progressBar.style.width = value + '%';
            progressBar.textContent = text;
        };

    })();
</script>
