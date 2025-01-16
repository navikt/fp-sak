package no.nav.foreldrepenger.web.app.tjenester.kodeverk.app;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
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
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagAndeltype;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;

@ApplicationScoped
public class HentKodeverkTjeneste {

    public static final Map<String, Collection<? extends Kodeverdi>> KODEVERDIER_SOM_BRUKES_PÅ_KLIENT;
    static {
        Map<String, Collection<? extends Kodeverdi>> map = new LinkedHashMap<>();

        map.put(RelasjonsRolleType.class.getSimpleName(), RelasjonsRolleType.kodeMap().values());
        map.put(FagsakStatus.class.getSimpleName(), FagsakStatus.kodeMap().values());
        map.put(FagsakMarkering.class.getSimpleName(), FagsakMarkering.kodeMap().values());
        map.put(BehandlingÅrsakType.class.getSimpleName(), BehandlingÅrsakType.kodeMap().values());
        map.put(KlageMedholdÅrsak.class.getSimpleName(), KlageMedholdÅrsak.kodeMap().values());
        map.put(KlageAvvistÅrsak.class.getSimpleName(), KlageAvvistÅrsak.kodeMap().values());
        map.put(KlageVurdering.class.getSimpleName(), KlageVurdering.kodeMap().values());
        map.put(KlageVurderingOmgjør.class.getSimpleName(), KlageVurderingOmgjør.kodeMap().values());
        map.put(KlageHjemmel.class.getSimpleName(), KlageHjemmel.kodeMap().values());
        map.put(OppgaveÅrsak.class.getSimpleName(), OppgaveÅrsak.kodeMap().values());
        map.put(DokumentTypeId.class.getSimpleName(), DokumentTypeId.kodeMap().values());
        map.put(MedlemskapManuellVurderingType.class.getSimpleName(), filtrerMedlemskapManuellVurderingType(MedlemskapManuellVurderingType.kodeMap().values()));
        map.put(BehandlingResultatType.class.getSimpleName(), BehandlingResultatType.kodeMap().values());
        map.put(VergeType.class.getSimpleName(), VergeType.kodeMap().values());
        map.put(VirksomhetType.class.getSimpleName(), VirksomhetType.kodeMap().values());
        map.put(PersonstatusType.class.getSimpleName(), PersonstatusType.kodeMap().values());
        map.put(FagsakYtelseType.class.getSimpleName(), FagsakYtelseType.kodeMap().values());
        map.put(FamilieHendelseType.class.getSimpleName(), FamilieHendelseType.kodeMap().values());
        map.put(Venteårsak.class.getSimpleName(), Venteårsak.kodeMap().values());
        map.put(ForeldreType.class.getSimpleName(), ForeldreType.kodeMap().values());
        map.put(InnsynResultatType.class.getSimpleName(), InnsynResultatType.kodeMap().values());
        map.put(BehandlingType.class.getSimpleName(), BehandlingType.kodeMap().values());
        map.put(Språkkode.class.getSimpleName(), Språkkode.kodeMap().values());
        map.put(Region.class.getSimpleName(), Region.kodeMap().values());
        map.put(Landkoder.class.getSimpleName(), Landkoder.kodeMap().values());
        map.put(ArbeidType.class.getSimpleName(), filtrerArbeidType(ArbeidType.kodeMap().values()));
        map.put(PeriodeResultatÅrsak.class.getSimpleName(), PeriodeResultatÅrsak.kodeMap().values());
        map.put(OpptjeningAktivitetType.class.getSimpleName(), OpptjeningAktivitetType.kodeMap().values());
        map.put(RevurderingVarslingÅrsak.class.getSimpleName(), RevurderingVarslingÅrsak.kodeMap().values());
        map.put(Inntektskategori.class.getSimpleName(), Inntektskategori.kodeMap().values());
        map.put(BeregningsgrunnlagAndeltype.class.getSimpleName(), BeregningsgrunnlagAndeltype.kodeMap().values());
        map.put(AktivitetStatus.class.getSimpleName(), AktivitetStatus.kodeMap().values());
        map.put(Arbeidskategori.class.getSimpleName(), Arbeidskategori.kodeMap().values());
        map.put(OmsorgsovertakelseVilkårType.class.getSimpleName(), OmsorgsovertakelseVilkårType.kodeMap().values());
        map.put(Fagsystem.class.getSimpleName(), Fagsystem.kodeMap().values());
        map.put(SivilstandType.class.getSimpleName(), SivilstandType.kodeMap().values());
        map.put(FaktaOmBeregningTilfelle.class.getSimpleName(), FaktaOmBeregningTilfelle.kodeMap().values());
        map.put(GraderingAvslagÅrsak.class.getSimpleName(), GraderingAvslagÅrsak.kodeMap().values());
        map.put(SkjermlenkeType.class.getSimpleName(), SkjermlenkeType.kodeMap().values());
        map.put(ArbeidsforholdHandlingType.class.getSimpleName(), ArbeidsforholdHandlingType.kodeMap().values());
        map.put(HistorikkAktør.class.getSimpleName(), HistorikkAktør.kodeMap().values());
        map.put(BehandlingStatus.class.getSimpleName(), BehandlingStatus.kodeMap().values());
        map.put(FarSøkerType.class.getSimpleName(), FarSøkerType.kodeMap().values());
        map.put(MedlemskapDekningType.class.getSimpleName(), MedlemskapDekningType.kodeMap().values());
        map.put(MedlemskapType.class.getSimpleName(), MedlemskapType.kodeMap().values());
        map.put(OppholdstillatelseType.class.getSimpleName(), OppholdstillatelseType.kodeMap().values());
        map.put(Avslagsårsak.class.getSimpleName(), Avslagsårsak.kodeMap().values());
        map.put(StønadskontoType.class.getSimpleName(), StønadskontoType.kodeMap().values());
        map.put(KonsekvensForYtelsen.class.getSimpleName(), KonsekvensForYtelsen.kodeMap().values());
        map.put(VilkårType.class.getSimpleName(), VilkårType.kodeMap().values());
        map.put(PermisjonsbeskrivelseType.class.getSimpleName(), PermisjonsbeskrivelseType.kodeMap().values());
        map.put(AnkeVurdering.class.getSimpleName(), AnkeVurdering.kodeMap().values());
        map.put(AnkeVurderingOmgjør.class.getSimpleName(), AnkeVurderingOmgjør.kodeMap().values());
        map.put(AnkeOmgjørÅrsak.class.getSimpleName(), AnkeOmgjørÅrsak.kodeMap().values());
        map.put(TilbakekrevingVidereBehandling.class.getSimpleName(), TilbakekrevingVidereBehandling.kodeMap().values());
        map.put(VurderÅrsak.class.getSimpleName(), VurderÅrsak.kodeMap().values());
        map.put(UttakUtsettelseType.class.getSimpleName(), UttakUtsettelseType.kodeMap().values());
        map.put(OppholdÅrsak.class.getSimpleName(), OppholdÅrsak.kodeMap().values());
        map.put(OverføringÅrsak.class.getSimpleName(), OverføringÅrsak.kodeMap().values());
        map.put(UtsettelseÅrsak.class.getSimpleName(), UtsettelseÅrsak.kodeMap().values());
        map.put(UttakArbeidType.class.getSimpleName(), UttakArbeidType.kodeMap().values());
        map.put(UttakPeriodeType.class.getSimpleName(), UttakPeriodeType.kodeMap().values());
        map.put(MorsAktivitet.class.getSimpleName(), MorsAktivitet.kodeMap().values());
        map.put(ManuellBehandlingÅrsak.class.getSimpleName(), ManuellBehandlingÅrsak.kodeMap().values());
        map.put(FaresignalVurdering.class.getSimpleName(), FaresignalVurdering.kodeMap().values());
        map.put(FordelingPeriodeKilde.class.getSimpleName(), FordelingPeriodeKilde.kodeMap().values());
        map.put(AdresseType.class.getSimpleName(), AdresseType.kodeMap().values());
        map.put(NaturalYtelseType.class.getSimpleName(), NaturalYtelseType.kodeMap().values());

        Map<String, Collection<? extends Kodeverdi>> mapFiltered = new LinkedHashMap<>();

        map.forEach((key, value) -> mapFiltered.put(key,
            value.stream().filter(f -> !"-".equals(f.getKode())).collect(Collectors.toSet())));

        KODEVERDIER_SOM_BRUKES_PÅ_KLIENT = Collections.unmodifiableMap(mapFiltered);

    }

    public HentKodeverkTjeneste() {
        // for CDI proxy
    }

    private static Collection<? extends Kodeverdi> filtrerMedlemskapManuellVurderingType(Collection<MedlemskapManuellVurderingType> values) {
        return values.stream().filter(MedlemskapManuellVurderingType::visesPåKlient).collect(Collectors.toSet());
    }

    private static Collection<? extends Kodeverdi> filtrerArbeidType(Collection<ArbeidType> values) {
        return values.stream().filter(ArbeidType::erAnnenOpptjening).collect(Collectors.toSet());
    }

    public Map<String, Collection<? extends Kodeverdi>> hentGruppertKodeliste() {

        return new LinkedHashMap<>(KODEVERDIER_SOM_BRUKES_PÅ_KLIENT);
    }

}
