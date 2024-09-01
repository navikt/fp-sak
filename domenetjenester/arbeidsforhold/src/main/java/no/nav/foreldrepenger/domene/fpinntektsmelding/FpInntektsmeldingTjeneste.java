package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ApplicationScoped
public class FpInntektsmeldingTjeneste {
    private FpinntektsmeldingKlient klient;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    FpInntektsmeldingTjeneste() {
        // CDI
    }

    @Inject
    public FpInntektsmeldingTjeneste(FpinntektsmeldingKlient klient, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.klient = klient;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void lagForespørselTask(String ag, BehandlingReferanse ref) {
        // Toggler av for prod og lokalt, ikke støtte lokalt
        if (!Environment.current().isDev()) {
            return;
        }
        var taskdata = ProsessTaskData.forTaskType(TaskType.forProsessTask(FpinntektsmeldingTask.class));
        taskdata.setBehandling(ref.fagsakId(), ref.behandlingId());
        taskdata.setCallIdFraEksisterende();
        taskdata.setProperty(FpinntektsmeldingTask.ARBEIDSGIVER_KEY, ag);
        prosessTaskTjeneste.lagre(taskdata);
    }

    void lagForespørsel(String ag, BehandlingReferanse ref, Skjæringstidspunkt stp) {
        // Toggler av for prod og lokalt, ikke støtte lokalt
        if (!Environment.current().isDev()) {
            return;
        }
        if (!OrganisasjonsNummerValidator.erGyldig(ag)) {
            return;
        }
        var request = new OpprettForespørselRequest(new OpprettForespørselRequest.AktørIdDto(ref.aktørId().getId()),
            new OpprettForespørselRequest.OrganisasjonsnummerDto(ag), stp.getUtledetSkjæringstidspunkt(), mapYtelsetype(ref.fagsakYtelseType()),
            new OpprettForespørselRequest.SaksnummerDto(ref.saksnummer().getVerdi()));
        klient.opprettForespørsel(request);
    }

    private OpprettForespørselRequest.YtelseType mapYtelsetype(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> OpprettForespørselRequest.YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> OpprettForespørselRequest.YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT,ENGANGSTØNAD -> throw new IllegalArgumentException("Kan ikke opprette forespørsel for ytelsetype " + fagsakYtelseType);
        };
    }
}
