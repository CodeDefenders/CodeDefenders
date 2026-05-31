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
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%@ page import="org.codedefenders.util.Paths" %>

<p:main_page title="${i18n.tr('User Analytics')}">
    <div class="container">
        <t:admin_navigation activePage="adminAnalytics"/>

        <h3>${i18n.tr('User Analytics')}</h3>

        <table id="tableUsers" class="table table-striped">
            <thead>
                <tr>
                    <th class="toggle-all-details"><i class="toggle-details-icon fa fa-chevron-right"></i></th>
                    <th>${i18n.tr('ID')}</th>
                    <th>${i18n.tr('Username')}</th>
                    <th>${i18n.tr('Games Played')}</th>
                    <th>${i18n.tr('Attacker Score')}</th>
                    <th>${i18n.tr('Defender Score')}</th>
                    <th>${i18n.tr('Total Score')}</th>
                </tr>
            </thead>
        </table>

        <div class="row mt-4">
            <div class="col-auto">
                <div class="btn-group">
                    <a download="user-analytics.csv" href="${url.forPath(Paths.API_ANALYTICS_USERS)}?fileType=csv"
                       type="button" class="btn btn-sm btn-outline-secondary" id="download">
                        <i class="fa fa-download me-1"></i>
                            ${i18n.tr('Download table')}
                    </a>
                    <a download="user-analytics.csv" href="${url.forPath(Paths.API_ANALYTICS_USERS)}?fileType=csv"
                       type="button" class="btn btn-sm btn-outline-secondary" id="download-csv">
                            ${i18n.tr('as CSV')}
                    </a>
                    <a download="user-analytics.json" href="${url.forPath(Paths.API_ANALYTICS_USERS)}?fileType=json"
                       type="button" class="btn btn-sm btn-outline-secondary" id="download-json">
                            ${i18n.tr('as JSON')}
                    </a>
                </div>
            </div>
        </div>
    </div>

    <script type="module">
        import DataTable from '${url.forPath("/js/datatables.mjs")}';
        import $ from '${url.forPath("/js/jquery.mjs")}';

        import {DataTablesUtils} from '${url.forPath("/js/codedefenders_main.mjs")}';


        const div = DataTablesUtils.formatDivision;
        const valPercent = DataTablesUtils.formatValueAndPercent;

        function format(data) {
            return '' +
                `<div class="child-row-wrapper">
                    <table class="child-row-details">
                        <thead>
                            <tr>
                                <th>${i18n.tr('Games Played')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>${i18n.tr('Games as Attacker:')}</td>
                                <td>\${valPercent(data.attackerGamesPlayed, data.gamesPlayed)}</td>
                                <td>${i18n.tr('Games as Defender:')}</td>
                                <td>\${valPercent(data.defenderGamesPlayed, data.gamesPlayed)}</td>
                            </tr>
                        </tbody>
                        <thead>
                            <tr>
                                <th>${i18n.tr('Mutants')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>${i18n.tr('Mutants Submitted:')}</td>
                                <td>\${data.mutantsSubmitted}</td>
                                <td>${i18n.tr('Per Game (as Attacker):')}</td>
                                <td>\${div(data.mutantsSubmitted, data.attackerGamesPlayed)}</td>
                            </tr>
                            <tr>
                                <td>${i18n.tr('Alive Mutants:')}</td>
                                <td>\${valPercent(data.mutantsAlive, data.mutantsSubmitted)}</td>
                                <td>${i18n.tr('Per Game (as Attacker):')}</td>
                                <td>\${div(data.mutantsAlive, data.attackerGamesPlayed)}</td>
                            </tr>
                            <tr>
                                <td>${i18n.tr('Equivalent Mutants:')}</td>
                                <td>\${valPercent(data.mutantsEquivalent, data.mutantsSubmitted)}</td>
                                <td>${i18n.tr('Per Game (as Attacker):')}</td>
                                <td>\${div(data.mutantsEquivalent, data.attackerGamesPlayed)}</td>
                            </tr>
                        </tbody>
                        <thead>
                            <tr>
                                <th>${i18n.tr('Tests')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>${i18n.tr('Tests Submitted:')}</td>
                                <td>\${data.testsSubmitted}</td>
                                <td>${i18n.tr('Per Game (as Defender):')}</td>
                                <td>\${div(data.testsSubmitted, data.defenderGamesPlayed)}</td>
                            </tr>
                            <tr>
                                <td>${i18n.tr('Mutants Killed:')}</td>
                                <td>\${data.mutantsKilled}</td>
                                <td>${i18n.tr('Per Game (as Defender):')}</td>
                                <td>\${div(data.mutantsKilled, data.defenderGamesPlayed)}</td>
                                <td>${i18n.tr('Per Test:')}</td>
                                <td>\${div(data.mutantsKilled, data.testsSubmitted)}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>`;
        }

        $(document).ready(function() {
            const table = new DataTable('#tableUsers', {
                "ajax": {
                    "url": "${url.forPath(Paths.API_ANALYTICS_USERS)}?fileType=json",
                    "dataSrc": "data"
                },
                "columns": [
                    {
                        "className":      'toggle-details',
                        "orderable":      false,
                        "data":           null,
                        "defaultContent": '<i class="toggle-details-icon fa fa-chevron-right"></i>'
                    },
                    { "data": "id" },
                    { "data": "username" },
                    { "data": "gamesPlayed" },
                    { "data": "attackerScore" },
                    { "data": "defenderScore" },
                    { "data":
                        function (row, type, val, meta) {
                            return row.attackerScore + row.defenderScore;
                        }
                    }
                ],
                "pageLength": 50,
                "order": [[ 1, "asc" ]],
                "scrollY": '600px',
                "scrollCollapse": true,
                "paging": false,
                "language": DataTablesUtils.language({"info": "${i18n.tr('Showing _TOTAL_ entries')}"})
            });

            DataTablesUtils.setupChildRows(table, format);
        });
    </script>
</p:main_page>
