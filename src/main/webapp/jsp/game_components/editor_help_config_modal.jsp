<%--

    Copyright (C) 2016-2025 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<div id="editor-help-modal" class="modal fade" role="dialog" tabindex="-1" aria-labelledby="editor-help-modal-title" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="editor-help-modal-title">${i18n.tr("Editor Keyboard Shortcuts")}</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"
                        aria-label="${i18n.tr('Close')}"></button>
            </div>

            <div class="modal-body">
                <table class="table table-striped table-condensed">
                    <thead>
                        <tr>
                            <th>${i18n.tr("Action")}</th>
                            <th>${i18n.tr("Key")}</th>
                            <th>${i18n.tr("Key (Mac)")}</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>${i18n.tr("Autocomplete")}</td>
                            <td>${i18n.tr("Ctrl + Space")}</td>
                            <td>${i18n.tr("Cmd + Space")}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr("Search")}</td>
                            <td>${i18n.tr("Ctrl + F")}</td>
                            <td>${i18n.tr("Cmd + F")}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr("Find Next")}</td>
                            <td>${i18n.tr("Ctrl + G")}</td>
                            <td>${i18n.tr("Cmd + G")}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr("Find Previous")}</td>
                            <td>${i18n.tr("Ctrl + Shift + G")}</td>
                            <td>${i18n.tr("Cmd + Shift + G")}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr("Search and Replace")}</td>
                            <td>${i18n.tr("Ctrl + Shift + F")}</td>
                            <td>${i18n.tr("Cmd + Shift + F")}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr("Search and Replace All")}</td>
                            <td>${i18n.tr("Ctrl + Shift + R")}</td>
                            <td>${i18n.tr("Cmd + Shift + R")}</td>
                        </tr>
                        <tr>
                            <td>${i18n.tr("Jump to Line")}</td>
                            <td>${i18n.tr("Alt + R")}</td>
                            <td>${i18n.tr("Alt + R")}</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">${i18n.tr("Close")}</button>
            </div>
        </div>
    </div>
</div>
