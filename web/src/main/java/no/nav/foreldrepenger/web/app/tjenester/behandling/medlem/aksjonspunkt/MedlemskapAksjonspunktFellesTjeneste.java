package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class MedlemskapAksjonspunktFellesTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public MedlemskapAksjonspunktFellesTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository,
                                                BehandlingRepository behandlingRepository) {
        this.historikkAdapter = historikkAdapter;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.medlemskapVilkårPeriodeRepository = medlemskapVilkårPeriodeRepository;
        this.behandlingRepository = behandlingRepository;
    }

    MedlemskapAksjonspunktFellesTjeneste() {
        //CDI
    }

    public OppdateringResultat oppdater(long behandlingId,
                                        Avslagsårsak avslagsårsak,
                                        LocalDate opphørFom,
                                        String begrunnelse,
                                        SkjermlenkeType skjermlenkeType) {
        if (avslagsårsak != null && !VilkårType.MEDLEMSKAPSVILKÅRET.getAvslagsårsaker().contains(avslagsårsak)) {
            throw new IllegalArgumentException("Ugyldig avslagsårsak for medlemskapsvilkåret");
        }
        var utfall = avslagsårsak == null || erOpphørEtterStp(behandlingId, opphørFom) ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        lagHistorikkInnslag(utfall, begrunnelse, opphørFom, skjermlenkeType);

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var grBuilder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        var periodeBuilder = MedlemskapsvilkårPeriodeEntitet.Builder.oppdatere(Optional.empty());
        periodeBuilder.opprettOverstyring(opphørFom, avslagsårsak, avslagsårsak == null ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT);
        grBuilder.medMedlemskapsvilkårPeriode(periodeBuilder);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder);

        if (VilkårUtfallType.OPPFYLT.equals(utfall)) {
            return oppfyltResultat();
        }
        return OppdateringResultat.utenTransisjon()
            .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
            .leggTilManueltAvslåttVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, avslagsårsak)
            .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
            .build();
    }

    public VilkårUtfallType oppdaterForutgående(long behandlingId,
                                                Avslagsårsak avslagsårsak,
                                                LocalDate medlemFom,
                                                String begrunnelse,
                                                SkjermlenkeType skjermlenkeType) {
        if (avslagsårsak != null && !VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE.getAvslagsårsaker().contains(avslagsårsak)) {
            throw new IllegalArgumentException("Ugyldig avslagsårsak for medlemskapsvilkåret");
        }
        var utfall = avslagsårsak == null ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        lagHistorikkInnslagForutgående(utfall, begrunnelse, medlemFom, skjermlenkeType);

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var grBuilder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        var periodeBuilder = MedlemskapsvilkårPeriodeEntitet.Builder.oppdatere(Optional.empty());
        periodeBuilder.opprettOverstyring(medlemFom, avslagsårsak, utfall);
        grBuilder.medMedlemskapsvilkårPeriode(periodeBuilder);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder);

        return utfall;
    }

    private void lagHistorikkInnslag(VilkårUtfallType nyVerdi, String begrunnelse, LocalDate opphørFom, SkjermlenkeType skjermlenkeType) {
        var historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.MEDLEMSKAPSVILKÅRET, null, nyVerdi)
            .medBegrunnelse(begrunnelse).medSkjermlenke(skjermlenkeType);

        if (nyVerdi.equals(VilkårUtfallType.OPPFYLT) && opphørFom != null) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.MEDLEMSKAPSVILKÅRET_OPPHØRSDATO, null, opphørFom);
        }
    }

    private void lagHistorikkInnslagForutgående(VilkårUtfallType nyVerdi, String begrunnelse, LocalDate medlemFom, SkjermlenkeType skjermlenkeType) {
        var historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.MEDLEMSKAPSVILKÅRET, null, nyVerdi)
            .medBegrunnelse(begrunnelse).medSkjermlenke(skjermlenkeType);

        if (VilkårUtfallType.IKKE_OPPFYLT.equals(nyVerdi) && medlemFom != null) {
            historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.MEDLEMSKAPSVILKÅRET_MEDLEMFRADATO, null, medlemFom);
        }
    }

    private static OppdateringResultat oppfyltResultat() {
        return new OppdateringResultat.Builder().leggTilManueltOppfyltVilkår(VilkårType.MEDLEMSKAPSVILKÅRET).build();
    }

    private boolean erOpphørEtterStp(Long behandlingId, LocalDate opphørFom) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
        return opphørFom != null && opphørFom.isAfter(stp);
    }

}
