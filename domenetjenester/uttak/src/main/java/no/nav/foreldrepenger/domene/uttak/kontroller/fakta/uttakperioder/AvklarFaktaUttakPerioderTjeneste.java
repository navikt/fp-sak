package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType.erFarEllerMedmor;
import static no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.UttakPeriodeEndringDto.TypeEndring.AVKLART;
import static no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.UttakPeriodeEndringDto.TypeEndring.ENDRET;
import static no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.UttakPeriodeEndringDto.TypeEndring.LAGT_TIL;
import static no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.UttakPeriodeEndringDto.TypeEndring.SLETTET;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.wagnerfisher.EditDistanceOperasjon;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.wagnerfisher.WagnerFisher;

@ApplicationScoped
public class AvklarFaktaUttakPerioderTjeneste {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public AvklarFaktaUttakPerioderTjeneste(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    AvklarFaktaUttakPerioderTjeneste() {
        // For CDI
    }

    public KontrollerFaktaData hentKontrollerFaktaPerioder(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        Long behandlingId = ref.getBehandlingId();
        Optional<YtelseFordelingAggregat> ytelseFordeling = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);

        if (ytelseFordeling.isEmpty()) {
            return utenGrunnlagResultat();
        }

        LocalDate fødselsDatoTilTidligOppstart = utledDatoForTidligOppstart(input);
        return SøknadsperiodeDokumentasjonKontrollerer.kontrollerPerioder(ytelseFordeling.get(), fødselsDatoTilTidligOppstart);
    }

    private KontrollerFaktaData utenGrunnlagResultat() {
        return new KontrollerFaktaData(List.of());
    }

    private LocalDate utledDatoForTidligOppstart(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        FamilieHendelser familieHendelser = fpGrunnlag.getFamilieHendelser();
        if (!avklartFørsteUttaksdato(ref.getBehandlingId()) && erFarEllerMedmor(ref.getRelasjonsRolleType()) && familieHendelser.gjelderTerminFødsel()) {
            return familieHendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato();
        }
        return null;
    }

    private boolean avklartFørsteUttaksdato(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregat(behandlingId).getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato).isPresent();
    }

    boolean finnesOverlappendePerioder(Long behandlingId) {
        Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
        if (ytelseFordelingAggregat.isPresent()) {
            YtelseFordelingAggregat ytelseFordeling = ytelseFordelingAggregat.get();
            List<OppgittPeriodeEntitet> perioder = ytelseFordeling.getGjeldendeSøknadsperioder().getOppgittePerioder()
                .stream()
                .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
                .collect(Collectors.toList());

            for (int i = 0; i < perioder.size() - 1; i++) {
                OppgittPeriodeEntitet oppgittPeriode = perioder.get(i);
                OppgittPeriodeEntitet nestePeriode = perioder.get(i + 1);
                if (!nestePeriode.getFom().isAfter(oppgittPeriode.getTom())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<UttakPeriodeEndringDto> finnEndringMellomOppgittOgGjeldendePerioder(YtelseFordelingAggregat ytelseFordelingAggregat) {
        List<UttakPeriodeEditDistance> oppgittePerioder = ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()
            .stream()
            .map(UttakPeriodeEditDistance::new)
            .sorted(Comparator.comparing(p -> p.getPeriode().getFom()))
            .collect(Collectors.toList());

        List<UttakPeriodeEditDistance> gjeldendePerioder = ytelseFordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder()
            .stream()
            .map(this::mapPeriode)
            .sorted(Comparator.comparing(u -> u.getPeriode().getFom()))
            .collect(Collectors.toList());

        List<EditDistanceOperasjon<UttakPeriodeEditDistance>> operasjoner = WagnerFisher.finnEnklesteSekvens(oppgittePerioder, gjeldendePerioder);

        return operasjoner.stream()
            .map(this::mapFra)
            .sorted(Comparator.comparing(UttakPeriodeEndringDto::getFom))
            .collect(Collectors.toList());
    }

    public List<UttakPeriodeEndringDto> finnEndringMellomOppgittOgGjeldendePerioder(Long aggregatId) {
        YtelseFordelingAggregat ytelseFordelingAggregat = ytelsesFordelingRepository.hentYtelsesFordelingPåId(aggregatId);
        return finnEndringMellomOppgittOgGjeldendePerioder(ytelseFordelingAggregat);
    }

    public List<UttakPeriodeEndringDto> finnEndringMellomOppgittOgGjeldendePerioder(Behandling behandling) {
        YtelseFordelingAggregat ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandling.getId());
        return finnEndringMellomOppgittOgGjeldendePerioder(ytelseFordelingAggregat);
    }

    private UttakPeriodeEndringDto mapFra(EditDistanceOperasjon<UttakPeriodeEditDistance> operasjon) {
        final UttakPeriodeEndringDto.TypeEndring typeEndring;
        final OppgittPeriodeEntitet periode;

        if (operasjon.erSletteOperasjon()) {
            periode = operasjon.getFør().getPeriode();
            typeEndring = SLETTET;
        } else if (operasjon.erSettInnOperasjon()) {
            periode = operasjon.getNå().getPeriode();
            typeEndring = LAGT_TIL;
        } else {
            periode = operasjon.getNå().getPeriode();
            typeEndring = operasjon.getNå().isPeriodeDokumentert() == null ? ENDRET : AVKLART;
        }

        return new UttakPeriodeEndringDto.Builder()
            .medPeriode(periode.getFom(), periode.getTom())
            .medTypeEndring(typeEndring)
            .build();
    }

    UttakPeriodeEditDistance mapPeriode(OppgittPeriodeEntitet periode) {
        if (harKrevdAvklaringFraSaksbehandler(periode)) {
            return UttakPeriodeEditDistance.builder(periode)
                .medPeriodeErDokumentert(erDokumentert(periode))
                .build();
        }
        return new UttakPeriodeEditDistance(periode);
    }

    private boolean harKrevdAvklaringFraSaksbehandler(OppgittPeriodeEntitet periode) {
        return periode.getBegrunnelse().isPresent();
    }

    private boolean erDokumentert(OppgittPeriodeEntitet periode) {
        return Objects.equals(periode.getPeriodeVurderingType(), UttakPeriodeVurderingType.PERIODE_OK) ||
            Objects.equals(periode.getPeriodeVurderingType(), UttakPeriodeVurderingType.PERIODE_OK_ENDRET);
    }

}
