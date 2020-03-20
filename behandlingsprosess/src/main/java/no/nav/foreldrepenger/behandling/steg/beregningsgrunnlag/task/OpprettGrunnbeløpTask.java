package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task;


import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(OpprettGrunnbeløpTask.TASKNAME)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettGrunnbeløpTask implements ProsessTaskHandler {

    public static final String TASKNAME = "beregning.opprettGrunnbeløp";

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    OpprettGrunnbeløpTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettGrunnbeløpTask(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFraKofakber = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(behandlingId, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        if (grunnlagFraKofakber.isPresent()) {
            BeregningsgrunnlagEntitet bg = grunnlagFraKofakber.get().getBeregningsgrunnlag().orElseThrow(() -> new IllegalStateException("Skal ha beregningsgrunnlag"));
            Beløp grunnbeløp = bg.getGrunnbeløp();
            Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagUtenGrunnbeløp = beregningsgrunnlagRepository.hentGrunnlagUtenGrunnbeløp(behandlingId);
            grunnlagUtenGrunnbeløp.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
                .ifPresent(beregningsgrunnlagEntitet -> BeregningsgrunnlagEntitet.builder(beregningsgrunnlagEntitet).medGrunnbeløp(grunnbeløp).build());
            BeregningsgrunnlagGrunnlagBuilder builder = BeregningsgrunnlagGrunnlagBuilder.endre(grunnlagUtenGrunnbeløp);
            beregningsgrunnlagRepository.lagreForMigrering(behandlingId, builder, BeregningsgrunnlagTilstand.OPPRETTET);

        }
    }

}
