<div id="scoringTooltip" class="modal fade" role="dialog" style="z-index: 10000; position: absolute;">
    <div class="modal-dialog">
        <!-- Modal content-->
        <div class="modal-content" style="z-index: 10000; position: absolute; width: 150%; left:-15%;">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Scoring System</h4>
            </div>
            <div class="modal-body">
                <div class="row">
                    <div class="col-sm-6">
                        <h2 class="text-center" style="color: #884466;">Attackers</h2>
                    </div>
                    <div class="col-sm-6">
                        <h2 class="text-center" style="color: #446688;">Defenders</h2>
                    </div>
                </div>
                <br/>
                <h3 class="text-center">Get one point for:</h3>
                <div class="row">
                    <div class="col-sm-6">
                        <ul>
                            <li>Creating a mutant that survives one test</li>
                            <li>Every additional test that executes the mutant and does not kill it</li>
                        </ul>
                    </div>
                    <div class="col-sm-6">
                        <ul>
                            <li>Each mutant your test kills</li>
                            <li>Each point that mutant had accumulated</li>
                            <li>Each previously untouched mutant your test kills</li>
                        </ul>
                    </div>
                </div>

                <br/><br/>

                <h3 class="text-center">Equivalence Duels:</h3>
                <br/>
                <h5 class="text-center">Mutant is proven to <b>not</b> be equivalent:</h5>
                <div class="row">
                    <div class="col-sm-6">
                        <ul><li>Keep the mutant's points and get an extra point</li></ul>
                    </div>
                    <div class="col-sm-6">
                        <ul><li>Your score does not change</li></ul>
                    </div>
                    <br/><br/><br/>
                </div>
                <h5 class="text-center">Equivalence is accepted or no killing test is provided in time:</h5>
                <div class="row">
                    <div class="col-sm-6">
                        <ul><li>Lose the mutant's points</li></ul>
                    </div>
                    <div class="col-sm-6">
                        <ul><li>Gain one additional point</li></ul>
                    </div>
                    <br/><br/>
                </div>
            </div>

        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        </div>
    </div>
</div>