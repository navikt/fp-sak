package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Konto;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Kontoer;
import no.nav.foreldrepenger.stønadskonto.grensesnitt.Stønadsdager;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.Brukerrolle;

@ApplicationScoped
public class KontoerGrunnlagBygger {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private RettOgOmsorgGrunnlagBygger rettOgOmsorgGrunnlagBygger;
    private DekningsgradTjeneste dekningsgradTjeneste;

    @Inject
    public KontoerGrunnlagBygger(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                 RettOgOmsorgGrunnlagBygger rettOgOmsorgGrunnlagBygger,
                                 DekningsgradTjeneste dekningsgradTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.rettOgOmsorgGrunnlagBygger = rettOgOmsorgGrunnlagBygger;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    KontoerGrunnlagBygger() {
        //CDI
    }

    /*
     * Ved siden av kontoer kan grunnlaget inneholde enten utenAktivitetskravDager eller minsterettDager, men ikke begge
     *
     * utenAktivitetskravDager
     * - gir mulighet til å innvilge perioder selv om aktivitetskravet ikke er oppfylt
     * - vil ikke påvirke stønadsperioden dvs må tas ut fortløpende. Ingen utsettelse uten at aktivitetskrav oppfylt
     * - Skal alltid brukes på tilfelle som krever sammenhengende uttak
     *
     * minsterettDager
     * - gir mulighet til å innvilge perioder selv om aktivitetskravet ikke er oppfylt
     * - automatiske trekk pga manglende søkt, avslag mv vil ikke påvirke minsterett
     * - kan utsettes og  utvide stønadsperioden
     * - Brukes framfor utenAktivitetskravDager fom FAB
     */
    public Kontoer.Builder byggGrunnlag(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();
        var stønadskontoer = hentStønadskontoer(ref);
        return getBuilder(uttakInput, stønadskontoer).kontoList(stønadskontoer.stream()
            //Flerbarnsdager er stønadskontotype i stønadskontoberegningen, men ikke i fastsette perioder
            .filter(sk -> sk.getStønadskontoType().erStønadsdager()).map(this::map).toList());
    }

    private Konto.Builder map(Stønadskonto stønadskonto) {
        return new Konto.Builder().trekkdager(stønadskonto.getMaxDager()).type(UttakEnumMapper.map(stønadskonto.getStønadskontoType()));
    }

    private Set<Stønadskonto> hentStønadskontoer(BehandlingReferanse ref) {
        return fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer())
            .getGjeldendeStønadskontoberegning()
            .orElseThrow(() -> new IllegalArgumentException("Behandling mangler stønadskontoer"))
            .getStønadskontoer();
    }

    /*
     * TFP-4846 legge inn regler for minsterett i stønadskontoutregningen + bruke standardkonfigurasjon
     */
    private Kontoer.Builder getBuilder(UttakInput uttakInput, Set<Stønadskonto> stønadskontoer) {
        var ref = uttakInput.getBehandlingReferanse();

        ForeldrepengerGrunnlag foreldrepengerGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var erMor = RelasjonsRolleType.MORA.equals(ref.relasjonRolle());
        var rettOgOmsorg = rettOgOmsorgGrunnlagBygger.byggGrunnlag(uttakInput).build();

        var bareFarHarRett = rettOgOmsorg.getFarHarRett() && !rettOgOmsorg.getMorHarRett() && !rettOgOmsorg.getAleneomsorg();
        var morHarUføretrygd = rettOgOmsorg.getMorUføretrygd();
        var dekningsgrad = UttakEnumMapper.map(dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref));

        var antallBarn = foreldrepengerGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getAntallBarn();
        var flerbarnsdager = finnKontoVerdi(stønadskontoer, StønadskontoType.FLERBARNSDAGER).orElse(0);


        var gjelderFødsel = foreldrepengerGrunnlag.getFamilieHendelser().gjelderTerminFødsel();

        var familieHendelse = familieHendelse(foreldrepengerGrunnlag);
        var familieHendelseNesteSak = familieHendelseNesteSak(foreldrepengerGrunnlag);

        var minsterettutleder = Stønadsdager.instance(null);
        /* Bruk disse når det er migrert korrekt ned til UR
            var bareFarMinsterett = finnKontoVerdi(stønadskontoer, StønadskontoType.BARE_FAR_RETT)
              .orElseGet(() -> minsterettutleder.minsterettBareFarRett(familieHendelse, antallBarn, bareFarHarRett, morHarUføretrygd, dekningsgrad));
            var uføredager = finnKontoVerdi(stønadskontoer, StønadskontoType.UFØREDAGER)
              .orElseGet(() -> minsterettutleder.aktivitetskravUføredager(familieHendelse, bareFarHarRett, morHarUføretrygd, dekningsgrad));
         */
        var bareFarMinsterett = minsterettutleder.minsterettBareFarRett(familieHendelse, antallBarn, bareFarHarRett, morHarUføretrygd, dekningsgrad);
        var uføredager = minsterettutleder.aktivitetskravUføredager(familieHendelse, bareFarHarRett, morHarUføretrygd, dekningsgrad);
        var farRundtFødsel = finnKontoVerdi(stønadskontoer, StønadskontoType.FAR_RUNDT_FØDSEL)
            .orElseGet(() -> minsterettutleder.andredagerFarRundtFødsel(familieHendelse, gjelderFødsel && rettOgOmsorg.getFarHarRett()));
        int toTette = 0;
        if (erMor) {
            toTette = finnKontoVerdi(stønadskontoer, StønadskontoType.TETTE_SAKER_MOR)
                .orElseGet(() -> minsterettutleder.minsterettTetteFødsler(Brukerrolle.MOR, gjelderFødsel, familieHendelse, familieHendelseNesteSak.orElse(null)));
        } else {
            toTette = finnKontoVerdi(stønadskontoer, StønadskontoType.TETTE_SAKER_FAR)
                .orElseGet(() -> minsterettutleder.minsterettTetteFødsler(Brukerrolle.FAR, gjelderFødsel, familieHendelse, familieHendelseNesteSak.orElse(null)));
        }
        return new Kontoer.Builder()
            .flerbarnsdager(flerbarnsdager)
            .minsterettDager(bareFarMinsterett)
            .utenAktivitetskravDager(uføredager)
            .farUttakRundtFødselDager(farRundtFødsel)
            .etterNesteStønadsperiodeDager(toTette);
    }

    private LocalDate familieHendelse(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return foreldrepengerGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato();
    }

    private Optional<LocalDate> familieHendelseNesteSak(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return foreldrepengerGrunnlag.getNesteSakGrunnlag().map(NesteSakGrunnlagEntitet::getHendelsedato);
    }

    private Optional<Integer> finnKontoVerdi(Set<Stønadskonto> konti, StønadskontoType stønadskontoType) {
        return konti.stream().filter(s -> stønadskontoType.equals(s.getStønadskontoType())).findFirst().map(Stønadskonto::getMaxDager);
    }
}
