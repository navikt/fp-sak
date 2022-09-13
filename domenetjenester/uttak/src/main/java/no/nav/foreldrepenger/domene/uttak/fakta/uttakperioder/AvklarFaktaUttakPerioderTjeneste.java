package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType.erFarEllerMedmor;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.fakta.wagnerfisher.EditDistanceOperasjon;
import no.nav.foreldrepenger.domene.uttak.fakta.wagnerfisher.WagnerFisher;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

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
        var behandlingId = ref.behandlingId();
        var ytelseFordeling = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);

        if (ytelseFordeling.isEmpty()) {
            return utenGrunnlagResultat();
        }

        var fødselsDatoTilTidligOppstart = utledDatoForTidligOppstart(input);

        return SøknadsperiodeDokKontrollerer.kontrollerPerioder(ytelseFordeling.get(), fødselsDatoTilTidligOppstart, input);
    }

    private KontrollerFaktaData utenGrunnlagResultat() {
        return new KontrollerFaktaData(List.of());
    }

    private LocalDate utledDatoForTidligOppstart(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        var familieHendelser = fpGrunnlag.getFamilieHendelser();
        if (!avklartFørsteUttaksdato(ref.behandlingId()) && erFarEllerMedmor(ref.relasjonRolle()) && familieHendelser.gjelderTerminFødsel()) {
            var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
            // Far/medmor med aleneomsorg kan begynne etter fødsel, ref Uttaksregler / Foreldrepenger
            if (UttakOmsorgUtil.harAleneomsorg(ytelseFordelingAggregat)) {
                return null;
            }
            return familieHendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato();
        }
        return null;
    }

    private boolean avklartFørsteUttaksdato(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregat(behandlingId).getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato).isPresent();
    }

    boolean finnesOverlappendePerioder(Long behandlingId) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
        if (ytelseFordelingAggregat.isPresent()) {
            var ytelseFordeling = ytelseFordelingAggregat.get();
            var perioder = ytelseFordeling.getGjeldendeSøknadsperioder().getOppgittePerioder()
                .stream()
                .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
                .collect(Collectors.toList());

            for (var i = 0; i < perioder.size() - 1; i++) {
                var oppgittPeriode = perioder.get(i);
                var nestePeriode = perioder.get(i + 1);
                if (!nestePeriode.getFom().isAfter(oppgittPeriode.getTom())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<UttakPeriodeEndringDto> finnEndringMellomOppgittOgGjeldendePerioder(YtelseFordelingAggregat ytelseFordelingAggregat) {
        var oppgittePerioder = ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()
            .stream()
            .map(UttakPeriodeEditDistance::new)
            .sorted(Comparator.comparing(p -> p.getPeriode().getFom()))
            .collect(Collectors.toList());

        var gjeldendePerioder = ytelseFordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder()
            .stream()
            .map(this::mapPeriode)
            .sorted(Comparator.comparing(u -> u.getPeriode().getFom()))
            .collect(Collectors.toList());

        var operasjoner = WagnerFisher.finnEnklesteSekvens(oppgittePerioder, gjeldendePerioder);

        return operasjoner.stream()
            .map(this::mapFra)
            .sorted(Comparator.comparing(UttakPeriodeEndringDto::getFom))
            .collect(Collectors.toList());
    }

    public List<UttakPeriodeEndringDto> finnEndringMellomOppgittOgGjeldendePerioder(Long aggregatId) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentYtelsesFordelingPåId(aggregatId);
        return finnEndringMellomOppgittOgGjeldendePerioder(ytelseFordelingAggregat);
    }

    public List<UttakPeriodeEndringDto> finnEndringMellomOppgittOgGjeldendePerioderForBehandling(Long behandlingId) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        return finnEndringMellomOppgittOgGjeldendePerioder(ytelseFordelingAggregat);
    }

    private UttakPeriodeEndringDto mapFra(EditDistanceOperasjon<UttakPeriodeEditDistance> operasjon) {
        final UttakPeriodeEndringDto.TypeEndring typeEndring;
        final OppgittPeriodeEntitet periode;

        if (operasjon.erSletteOperasjon()) {
            periode = operasjon.getFør().getPeriode();
            typeEndring = UttakPeriodeEndringDto.TypeEndring.SLETTET;
        } else if (operasjon.erSettInnOperasjon()) {
            periode = operasjon.getNå().getPeriode();
            typeEndring = UttakPeriodeEndringDto.TypeEndring.LAGT_TIL;
        } else {
            periode = operasjon.getNå().getPeriode();
            typeEndring = operasjon.getNå().isPeriodeDokumentert() == null ? UttakPeriodeEndringDto.TypeEndring.ENDRET : UttakPeriodeEndringDto.TypeEndring.AVKLART;
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
