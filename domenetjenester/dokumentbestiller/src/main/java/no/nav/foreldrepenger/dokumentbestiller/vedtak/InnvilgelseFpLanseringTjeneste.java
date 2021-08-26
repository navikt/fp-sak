package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InnvilgelseFpLanseringTjeneste {
    private static final Logger LOGGER = LoggerFactory.getLogger(InnvilgelseFpLanseringTjeneste.class);
    private static final Environment ENV = Environment.current();
    private static final List<String> GRADERINGS_ÅRSAKER = List.of("4025", "4080", "4093", "4094");

    private ForeldrepengerUttakTjeneste fpUttakRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private SøknadRepository søknadRepository;

    @Inject
    public InnvilgelseFpLanseringTjeneste(ForeldrepengerUttakTjeneste fpUttakRepository,
                                          FamilieHendelseRepository familieHendelseRepository,
                                          SøknadRepository søknadRepository) {
        this.fpUttakRepository = fpUttakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.søknadRepository = søknadRepository;
    }

    InnvilgelseFpLanseringTjeneste() {
        //CDI
    }

    public DokumentMalType velgFpInnvilgelsesmal(Behandling behandling) {
        return ENV.isProd() ? velgFpInnvilgelsesmalProd(behandling) : velgFpInnvilgelsesmalDev(behandling);
    }

    public DokumentMalType velgFpInnvilgelsesmalProd(Behandling behandling) {
        boolean lansertDokGen = behandling.erYtelseBehandling()
            && !harPerioderMedGraderingEllerSamtidigUttak(behandling)
            && !harPerioderMedGradertOgAvslått(behandling)
            && !harDødtBarn(behandling)
            && harSøknadMedSpråkkodeNB(behandling);

        loggSaksnummerForNesteLansering(behandling);

        return lansertDokGen ?
            DokumentMalType.INNVILGELSE_FORELDREPENGER : DokumentMalType.INNVILGELSE_FORELDREPENGER_DOK;
    }

    public DokumentMalType velgFpInnvilgelsesmalDev(Behandling behandling) {
        return (!harDødtBarn(behandling) && harSøknadMedSpråkkodeNB(behandling)) ?
            DokumentMalType.INNVILGELSE_FORELDREPENGER : DokumentMalType.INNVILGELSE_FORELDREPENGER_DOK;
    }

    private void loggSaksnummerForNesteLansering(Behandling behandling) {

        if (vilBrukeDokgenVedNesteLansering(behandling)) {
            LOGGER.info("Saksnummer {} vil bruke Dokgen ved neste lansering", behandling.getFagsak().getSaksnummer().getVerdi());
        }
    }

    private boolean vilBrukeDokgenVedNesteLansering(Behandling behandling) {
        return harPerioderMedGraderingEllerSamtidigUttak(behandling)
            && harPerioderMedGradertOgAvslått(behandling)
            && !harDødtBarn(behandling)
            && harSøknadMedSpråkkodeNB(behandling);
    }

    private boolean harPerioderMedGraderingEllerSamtidigUttak(Behandling behandling) {
        return fpUttakRepository.hentUttakHvisEksisterer(behandling.getId())
            .map(ForeldrepengerUttak::getGjeldendePerioder)
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch(p -> p.isGraderingInnvilget() || p.isSøktGradering() || p.isSamtidigUttak());
    }

    private boolean harPerioderMedGradertOgAvslått(Behandling behandling) {
        return fpUttakRepository.hentUttakHvisEksisterer(behandling.getId())
            .map(ForeldrepengerUttak::getGjeldendePerioder)
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch(p -> !p.isInnvilget() && GRADERINGS_ÅRSAKER.contains(p.getResultatÅrsak().getKode()));
    }

    private boolean harDødtBarn(Behandling behandling) {
        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlagEntitet = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        return familieHendelseGrunnlagEntitet.isPresent()
            && (familieHendelseGrunnlagEntitet.get().getGjeldendeVersjon().getInnholderDødtBarn()
            || familieHendelseGrunnlagEntitet.get().getGjeldendeVersjon().getInnholderDøfødtBarn());
    }

    private boolean harSøknadMedSpråkkodeNB(Behandling behandling) {
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .map(s -> Språkkode.NB.equals(s.getSpråkkode()))
            .orElse(false);
    }
}
