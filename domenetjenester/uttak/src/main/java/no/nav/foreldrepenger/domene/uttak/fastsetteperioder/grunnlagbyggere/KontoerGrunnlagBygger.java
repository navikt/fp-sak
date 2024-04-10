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
import no.nav.foreldrepenger.stønadskonto.regelmodell.Minsterett;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.BeregnMinsterettGrunnlag;

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
            .filter(sk -> !sk.getStønadskontoType().equals(StønadskontoType.FLERBARNSDAGER)).map(this::map).toList());
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
        var minsterett = !ref.getSkjæringstidspunkt().utenMinsterett();
        var rettOgOmsorg = rettOgOmsorgGrunnlagBygger.byggGrunnlag(uttakInput).build();

        var bareFarHarRett = rettOgOmsorg.getFarHarRett() && !rettOgOmsorg.getMorHarRett();
        var morHarUføretrygd = rettOgOmsorg.getMorUføretrygd();
        var dekningsgrad = UttakEnumMapper.map(dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref));

        var antallBarn = foreldrepengerGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getAntallBarn();
        var flerbarnsdager = stønadskontoer.stream()
            .filter(k -> StønadskontoType.FLERBARNSDAGER.equals(k.getStønadskontoType()))
            .findFirst()
            .map(Stønadskonto::getMaxDager)
            .orElse(0);
        var builder = new Kontoer.Builder()
            .flerbarnsdager(flerbarnsdager);

        var gjelderFødsel = foreldrepengerGrunnlag.getFamilieHendelser().gjelderTerminFødsel();

        var familieHendelse = familieHendelse(foreldrepengerGrunnlag);
        var familieHendelseNesteSak = familieHendelseNesteSak(foreldrepengerGrunnlag);

        var aleneomsorg = rettOgOmsorg.getAleneomsorg();
        var minsterettGrunnlag = new BeregnMinsterettGrunnlag.Builder()
            .bareFarHarRett(bareFarHarRett)
            .morHarUføretrygd(morHarUføretrygd)
            .mor(erMor)
            .gjelderFødsel(gjelderFødsel)
            .antallBarn(antallBarn)
            .aleneomsorg(aleneomsorg)
            .minsterett(minsterett)
            .familieHendelseDato(familieHendelse)
            .familieHendelseDatoNesteSak(familieHendelseNesteSak.orElse(null))
            .dekningsgrad(dekningsgrad)
            .build();
        var minsteretter = Minsterett.finnMinsterett(minsterettGrunnlag);
        builder.minsterettDager(minsteretter.getOrDefault(Minsterett.GENERELL_MINSTERETT, 0));
        builder.utenAktivitetskravDager(minsteretter.getOrDefault(Minsterett.UTEN_AKTIVITETSKRAV, 0));
        builder.farUttakRundtFødselDager(minsteretter.getOrDefault(Minsterett.FAR_UTTAK_RUNDT_FØDSEL, 0));
        builder.etterNesteStønadsperiodeDager(minsteretter.getOrDefault(Minsterett.TETTE_FØDSLER, 0));
        return builder;
    }

    private LocalDate familieHendelse(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return foreldrepengerGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato();
    }

    private Optional<LocalDate> familieHendelseNesteSak(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return foreldrepengerGrunnlag.getNesteSakGrunnlag().map(NesteSakGrunnlagEntitet::getHendelsedato);
    }
}
