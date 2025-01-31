package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Konto;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Kontoer;

@ApplicationScoped
public class KontoerGrunnlagBygger {

    public KontoerGrunnlagBygger() {
        // tom konstruktør
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
    public Kontoer.Builder byggGrunnlag(UttakInput uttakInput, Map<StønadskontoType, Integer> stønadskontoutregning) {
        var kontoer = stønadskontoutregning.entrySet().stream()
            .filter(k -> k.getKey().erStønadsdager())
            .map(k -> new Konto.Builder().trekkdager(k.getValue()).type(UttakEnumMapper.map(k.getKey())))
            .toList();
        return getBuilder(uttakInput, stønadskontoutregning).kontoList(kontoer);
    }

    private Kontoer.Builder getBuilder(UttakInput uttakInput, Map<StønadskontoType, Integer> stønadskontoer) {
        var erMor = RelasjonsRolleType.MORA.equals(uttakInput.getBehandlingReferanse().relasjonRolle());
        int toTette = erMor ? finnKontoVerdi(stønadskontoer, StønadskontoType.TETTE_SAKER_MOR) : finnKontoVerdi(stønadskontoer, StønadskontoType.TETTE_SAKER_FAR);
        return new Kontoer.Builder()
            .flerbarnsdager(finnKontoVerdi(stønadskontoer, StønadskontoType.FLERBARNSDAGER))
            .minsterettDager(finnKontoVerdi(stønadskontoer, StønadskontoType.BARE_FAR_RETT))
            .utenAktivitetskravDager(finnKontoVerdi(stønadskontoer, StønadskontoType.UFØREDAGER))
            .farUttakRundtFødselDager(finnKontoVerdi(stønadskontoer, StønadskontoType.FAR_RUNDT_FØDSEL))
            .etterNesteStønadsperiodeDager(toTette);
    }

    private Integer finnKontoVerdi(Map<StønadskontoType, Integer> konti, StønadskontoType stønadskontoType) {
        return konti.getOrDefault(stønadskontoType, 0);
    }
}
