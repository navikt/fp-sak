package no.nav.foreldrepenger.web.app.tjenester.kodeverk.app;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravPermisjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto.KodeverdiMedNavnDto;

public class HentKodeverdierTjeneste {

    public static final Map<String, List<KodeverdiMedNavnDto>> KODEVERDIER_SOM_BRUKES_PÅ_KLIENT = Map.ofEntries(
        lagEnumEntry(AdresseType.class),
        lagEnumEntry(AktivitetStatus.class),
        lagEnumEntry(AktivitetskravPermisjonType.class),
        lagEnumEntry(AnkeOmgjørÅrsak.class),
        lagEnumEntry(ArbeidType.class, ArbeidType::erAnnenOpptjening),
        lagEnumEntry(Arbeidskategori.class),
        lagEnumEntry(Avslagsårsak.class),
        lagEnumEntry(BehandlingResultatType.class),
        lagEnumEntry(BehandlingStatus.class),
        lagEnumEntry(BehandlingType.class),
        lagEnumEntry(BehandlingÅrsakType.class),
        lagEnumEntry(FagsakMarkering.class),
        lagEnumEntry(FagsakStatus.class),
        lagEnumEntry(FagsakYtelseType.class),
        lagEnumEntry(FaktaOmBeregningTilfelle.class),
        lagEnumEntry(FamilieHendelseType.class),
        lagEnumEntry(FaresignalVurdering.class),
        lagEnumEntry(FordelingPeriodeKilde.class),
        lagEnumEntry(ForeldreType.class),
        lagEnumEntry(GraderingAvslagÅrsak.class),
        lagEnumEntry(HistorikkAktør.class),
        lagEnumEntry(InnsynResultatType.class),
        lagEnumEntry(Inntektskategori.class),
        lagEnumEntry(KlageAvvistÅrsak.class),
        lagEnumEntry(KlageHjemmel.class),
        lagEnumEntry(KlageMedholdÅrsak.class),
        lagEnumEntry(KonsekvensForYtelsen.class),
        lagEnumEntry(Landkoder.class),
        lagEnumEntry(ManuellBehandlingÅrsak.class),
        lagEnumEntry(MedlemskapDekningType.class),
        lagEnumEntry(MedlemskapManuellVurderingType.class, MedlemskapManuellVurderingType::visesPåKlient),
        lagEnumEntry(MedlemskapType.class),
        lagEnumEntry(MorsAktivitet.class),
        lagEnumEntry(NaturalYtelseType.class),
        lagEnumEntry(OmsorgsovertakelseVilkårType.class),
        lagEnumEntry(OppgaveType.class),
        lagEnumEntry(OppholdstillatelseType.class),
        lagEnumEntry(OppholdÅrsak.class),
        lagEnumEntry(OpptjeningAktivitetType.class),
        lagEnumEntry(OverføringÅrsak.class),
        lagEnumEntry(PeriodeResultatÅrsak.class),
        lagEnumEntry(PermisjonsbeskrivelseType.class),
        lagEnumEntry(PersonstatusType.class),
        lagEnumEntry(Region.class),
        lagEnumEntry(RelasjonsRolleType.class),
        lagEnumEntry(RevurderingVarslingÅrsak.class),
        lagEnumEntry(SivilstandType.class),
        lagEnumEntry(SkjermlenkeType.class),
        lagEnumEntry(StønadskontoType.class),
        lagEnumEntry(UtsettelseÅrsak.class),
        lagEnumEntry(UttakArbeidType.class),
        lagEnumEntry(UttakPeriodeType.class),
        lagEnumEntry(UttakUtsettelseType.class),
        lagEnumEntry(Venteårsak.class),
        lagEnumEntry(VergeType.class),
        lagEnumEntry(VilkårType.class),
        lagEnumEntry(VirksomhetType.class),
        lagEnumEntry(VurderÅrsak.class)
    );

    private HentKodeverdierTjeneste() {
    }

    private static Map.Entry<String, List<KodeverdiMedNavnDto>> lagEnumEntry(Class<? extends Kodeverdi> kodeverkClass) {
        return lagEnumEntry(kodeverkClass, _ -> true);
    }

    private static <K extends Kodeverdi> Map.Entry<String, List<KodeverdiMedNavnDto>> lagEnumEntry(Class<K> kodeverkClass, Predicate<K> filter) {
        if (!Enum.class.isAssignableFrom(kodeverkClass)) {
            throw new IllegalArgumentException("Ikke enum: " + kodeverkClass.getSimpleName());
        }
        var dtos = Arrays.stream(kodeverkClass.getEnumConstants())
            .filter(filter)
            .filter(kodeverdi -> !Kodeverdi.STANDARDKODE_UDEFINERT.equals(kodeverdi.getKode()))
            .map(k -> new KodeverdiMedNavnDto(k.getKode(), k.getNavn()))
            .toList();
        return Map.entry(kodeverkClass.getSimpleName(), dtos);
    }

    public static Map<String, List<KodeverdiMedNavnDto>> hentGruppertKodeliste() {
        return new LinkedHashMap<>(KODEVERDIER_SOM_BRUKES_PÅ_KLIENT);
    }

}
