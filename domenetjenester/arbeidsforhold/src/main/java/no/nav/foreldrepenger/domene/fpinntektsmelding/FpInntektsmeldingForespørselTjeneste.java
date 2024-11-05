package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class FpInntektsmeldingForespørselTjeneste {
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;

    FpInntektsmeldingForespørselTjeneste() {
        // CDI
    }

    @Inject
    public FpInntektsmeldingForespørselTjeneste(SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                YtelsesFordelingRepository ytelsesFordelingRepository,
                                                SvangerskapspengerRepository svangerskapspengerRepository) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
    }

    public OpprettForespørselRequest lagForespørsel(BehandlingReferanse ref, String arbeidsgiverIdent) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId());
        var aktørDto = new OpprettForespørselRequest.AktørIdDto(ref.aktørId().getId());
        var arbeidsgiverDto = new OpprettForespørselRequest.OrganisasjonsnummerDto(arbeidsgiverIdent);
        var saksnummerDto = new OpprettForespørselRequest.SaksnummerDto(ref.saksnummer().getVerdi());
        var søktePerioder = finnSøktePerioder(ref, arbeidsgiverIdent);
        return new OpprettForespørselRequest(aktørDto, arbeidsgiverDto, stp.getUtledetSkjæringstidspunkt(), mapYtelsetype(ref.fagsakYtelseType()),
            saksnummerDto, søktePerioder);
    }

    private List<OpprettForespørselRequest.Søknadsperiode> finnSøktePerioder(BehandlingReferanse ref, String arbeidsgiverIdent) {
        return switch (ref.fagsakYtelseType()) {
            case FORELDREPENGER -> hentSøktePerioderForeldrepenger(ref);
            case SVANGERSKAPSPENGER -> hentSøktePerioderSvangerskapspenger(ref, arbeidsgiverIdent);
            case UDEFINERT, ENGANGSTØNAD -> throw new IllegalArgumentException(String.format("Kan ikke finne søkte perioder for ytelsetype %s", ref.fagsakYtelseType()));
        };
    }

    private List<OpprettForespørselRequest.Søknadsperiode> hentSøktePerioderSvangerskapspenger(BehandlingReferanse ref, String arbeidsgiverIdent) {
        var tilrettelegginger = svangerskapspengerRepository.hentGrunnlag(ref.behandlingId())
            .map(SvpGrunnlagEntitet::getOpprinneligeTilrettelegginger)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .orElse(Collections.emptyList());
        var førsteFraværHosArbeidsgiver = tilrettelegginger.stream()
            .filter(tl -> tl.getArbeidsgiver().map(ag -> ag.getIdentifikator().equals(arbeidsgiverIdent)).orElse(false))
            .map(FpInntektsmeldingForespørselTjeneste::finnFørsteFom)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList().stream().min(Comparator.naturalOrder());

        // Forenkler perioden for SVP i første omgang, setter i praksis kun tidligste startdato
        return førsteFraværHosArbeidsgiver
            .map(dato -> Collections.singletonList(new OpprettForespørselRequest.Søknadsperiode(dato, Tid.TIDENES_ENDE)))
            .orElse(Collections.emptyList());
    }

    private static Optional<LocalDate> finnFørsteFom(SvpTilretteleggingEntitet tl) {
        return tl.getTilretteleggingFOMListe().stream().map(TilretteleggingFOM::getFomDato).min(Comparator.naturalOrder());
    }

    private List<OpprettForespørselRequest.Søknadsperiode> hentSøktePerioderForeldrepenger(BehandlingReferanse ref) {
        var ytelseaggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
        if (ytelseaggregat != null && ytelseaggregat.getOppgittFordeling() != null) {
            return ytelseaggregat.getOppgittFordeling().getPerioder().stream().map(yp -> new OpprettForespørselRequest.Søknadsperiode(yp.getFom(), yp.getTom())).toList();
        }
        return Collections.emptyList();
    }

    private OpprettForespørselRequest.YtelseType mapYtelsetype(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> OpprettForespørselRequest.YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> OpprettForespørselRequest.YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT, ENGANGSTØNAD -> throw new IllegalArgumentException(String.format("Kan ikke opprette forespørsel for ytelsetype %s",  fagsakYtelseType));
        };
    }

}
