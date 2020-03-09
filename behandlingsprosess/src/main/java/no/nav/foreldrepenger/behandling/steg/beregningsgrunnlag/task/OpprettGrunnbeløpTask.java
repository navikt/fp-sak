package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task;


import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
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
            grunnlagUtenGrunnbeløp.stream().flatMap(gr -> gr.getBeregningsgrunnlag().stream())
                .forEach(beregningsgrunnlagEntitet -> {
                    BeregningsgrunnlagEntitet build = BeregningsgrunnlagEntitet.builder(beregningsgrunnlagEntitet).medGrunnbeløp(grunnbeløp).build();
                    beregningsgrunnlagRepository.lagreForMigrering(behandlingId, build, BeregningsgrunnlagTilstand.OPPRETTET);
                });
        }
    }

}
