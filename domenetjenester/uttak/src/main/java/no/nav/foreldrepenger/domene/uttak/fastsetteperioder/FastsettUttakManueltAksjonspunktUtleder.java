package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@ApplicationScoped
public class FastsettUttakManueltAksjonspunktUtleder {

    private static final Set<String> ORGNR_TILKNYTTET_STORTINGET = Set.of(
        "971524960" //Stortingsrepresentant
        , "874707112" //Stortinget
        , "982110777" //Stortingets Kontrollutvalg for Etterretnings-, Overvåkings- Og Sikkerhetstjeneste, Eos-utvalget
        , "982164176" //Stortingets Kontrollutvalg for Etterretnings-, Overvåkings- Og Sikkerhetstjeneste, Eos-utvalget
        , "971527439" //Ombudsmannen for Forsvaret
        , "974721198" //Ombudsmannen for Forsvaret
        , "974760843" //Riksrevisjonen
        , "974707314" //Riksrevisjonen
        , "974761270" //Sivilombudsmannen Stortingets Ombudsmann for Forvaltningen
        , "974720590" //Sivilombudsmannen Stortingets Ombudsmann for Forvaltningen
        , "914781175" //Norges Nasjonale Institusjon for Menneskerettigheter Avd Kautokeino
        , "885002862" //Norges Nasjonale Institusjon for Menneskerettigheter Avd Kautokeino
        , "914781329" //Norges Nasjonale Institusjon for Menneskerettigheter Avd Oslo
    );

    private FpUttakRepository fpUttakRepository;

    @Inject
    FastsettUttakManueltAksjonspunktUtleder(UttakRepositoryProvider repositoryProvider){
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
    }

    FastsettUttakManueltAksjonspunktUtleder() {
        //CDI
    }

    public List<AksjonspunktResultat> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        Long behandlingId = ref.getBehandlingId();

        List<AksjonspunktResultat> aksjonspunktArray = new ArrayList<>();

        utledAksjonspunktForStortingsrepresentant(input).ifPresent(aksjonspunktArray::add);
        utledAksjonspunktForManuellBehandlingFraRegler(behandlingId).ifPresent(aksjonspunktArray::add);
        utledAksjonspunktForDødtBarn(input.getYtelsespesifiktGrunnlag()).ifPresent(aksjonspunktArray::add);

        return aksjonspunktArray.stream().distinct().collect(Collectors.toList());
    }

    private Optional<AksjonspunktResultat> utledAksjonspunktForManuellBehandlingFraRegler(Long behandlingId) {
        UttakResultatEntitet uttakResultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId).orElseThrow();
        for (UttakResultatPeriodeEntitet periode : uttakResultat.getGjeldendePerioder().getPerioder()) {
            if (periode.getResultatType().equals(PeriodeResultatType.MANUELL_BEHANDLING)){
                return Optional.of(fastsettUttakAksjonspunkt());
            }
        }
        return Optional.empty();
    }

    private Optional<AksjonspunktResultat> utledAksjonspunktForStortingsrepresentant(UttakInput input) {
        Collection<Yrkesaktivitet> yrkesaktiviteter = input.getYrkesaktiviteter().hentAlleYrkesaktiviteter();
        for (Yrkesaktivitet yrkesaktivitet : yrkesaktiviteter) {
            if (yrkesaktivitet.getArbeidsgiver() != null
                && yrkesaktivitet.getArbeidsgiver().getErVirksomhet()
                && aktivitetErTilknyttetStortinget(yrkesaktivitet)) {
                return Optional.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.TILKNYTTET_STORTINGET));
            }
        }
        return Optional.empty();
    }

    private Optional<AksjonspunktResultat> utledAksjonspunktForDødtBarn(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        if (finnesDødsdatoIRegistertEllerOverstyrtVersjon(foreldrepengerGrunnlag)) {
            return Optional.of(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD));
        }
        return Optional.empty();
    }

    private boolean finnesDødsdatoIRegistertEllerOverstyrtVersjon(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var familieHendelser = foreldrepengerGrunnlag.getFamilieHendelser();
        var barna = familieHendelser.getGjeldendeFamilieHendelse().getBarna();

        return barna.stream()
            .anyMatch(barn -> barn.getDødsdato().isPresent());
    }

    private AksjonspunktResultat fastsettUttakAksjonspunkt() {
        return AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER);
    }

    private boolean aktivitetErTilknyttetStortinget(Yrkesaktivitet yrkesaktivitet) {
        return ORGNR_TILKNYTTET_STORTINGET.contains(yrkesaktivitet.getArbeidsgiver().getOrgnr());
    }

}
