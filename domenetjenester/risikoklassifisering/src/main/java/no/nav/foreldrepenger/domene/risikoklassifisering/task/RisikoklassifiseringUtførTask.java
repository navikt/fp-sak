package no.nav.foreldrepenger.domene.risikoklassifisering.task;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.AktørId;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.Saksnummer;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.YtelseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.AnnenPartDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingRequestDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("risiko.klassifisering")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class RisikoklassifiseringUtførTask extends GenerellProsessTask {
    private RisikovurderingTjeneste risikovurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private PersonopplysningRepository personopplysningRepository;

    RisikoklassifiseringUtførTask() {
        // for CDI proxy
    }

    @Inject
    public RisikoklassifiseringUtførTask(RisikovurderingTjeneste risikovurderingTjeneste,
                                         BehandlingRepository behandlingRepository,
                                         SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                         OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                         PersonopplysningRepository personopplysningRepository) {
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);
        var request = opprettRequest(referanse);
        risikovurderingTjeneste.startRisikoklassifisering(referanse, request);
    }

    private RisikovurderingRequestDto opprettRequest(BehandlingReferanse ref) {
        var søkerAktørId = new AktørId(ref.aktørId().getId());
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId())
            .getUtledetSkjæringstidspunkt();
        var opplysningsperiode = opplysningsPeriodeTjeneste.beregn(ref.behandlingId(), ref.fagsakYtelseType());
        var konsumentId = ref.behandlingUuid();
        var ytelsetype = mapFagsaktype(ref.fagsakYtelseType());
        var annenPartOpt = mapAnnenPart(ref);
        return new RisikovurderingRequestDto(søkerAktørId, stp, opplysningsperiode.getFomDato(), opplysningsperiode.getTomDato(),
            konsumentId, ytelsetype, annenPartOpt.orElse(null), new Saksnummer(ref.saksnummer().getVerdi()));
    }

    private Optional<AnnenPartDto> mapAnnenPart(BehandlingReferanse ref) {
        var oppgittAnnenPart = personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(ref.behandlingId());
        if (oppgittAnnenPart.isPresent()) {
            var aktoerId =
                oppgittAnnenPart.get().getAktørId() == null ? null : oppgittAnnenPart.get().getAktørId().getId();
            if (aktoerId != null) {
                return Optional.of(new AnnenPartDto(new AktørId(aktoerId), null));
            }
            var utenlandskFnr = oppgittAnnenPart.get().getUtenlandskPersonident();
            if (utenlandskFnr != null) {
                return Optional.of(new AnnenPartDto(null, utenlandskFnr));
            }
        }
        return Optional.empty();
    }

    private YtelseType mapFagsaktype(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD -> YtelseType.ENGANGSSTØNAD;
            case UDEFINERT -> throw new IllegalStateException("Ugyldig FagsakYtelseType forsøkt mappet "
                + "til RisikovurderingRequest. FagsakYtelseType: " + fagsakYtelseType);
        };
    }

}
