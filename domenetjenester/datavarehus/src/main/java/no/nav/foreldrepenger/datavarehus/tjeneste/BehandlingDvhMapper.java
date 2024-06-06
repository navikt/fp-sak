package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VenteGruppe;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingDvh;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingMetode;
import no.nav.foreldrepenger.datavarehus.domene.RevurderingÅrsak;
import no.nav.foreldrepenger.datavarehus.domene.VilkårIkkeOppfylt;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

public class BehandlingDvhMapper {

    private BehandlingDvhMapper() {
    }

    public static BehandlingDvh map(Behandling behandling, // NOSONAR
                                    Behandlingsresultat behandlingsresultat,
                                    List<MottattDokument> mottatteDokument,
                                    Optional<BehandlingVedtak> vedtak,
                                    Optional<LocalDate> utbetaltTid,
                                    Optional<VilkårIkkeOppfylt> vilkårIkkeOppfylt,
                                    Optional<FamilieHendelseGrunnlagEntitet> fh,
                                    Optional<KlageResultatEntitet> klageResultat,
                                    Optional<AnkeResultatEntitet> ankeResultat,
                                    Optional<LocalDate> skjæringstidspunkt,
                                    FagsakMarkering fagsakMarkering,
                                    Optional<LocalDate> forventetOppstartDato) {

        var builder = BehandlingDvh.builder()
            .ansvarligBeslutter(behandling.getAnsvarligBeslutter())
            .ansvarligSaksbehandler(utledAnsvarligSaksbehandler(behandling))
            .behandlendeEnhet(behandling.getBehandlendeEnhet())
            .behandlingId(behandling.getId())
            .behandlingUuid(behandling.getUuid())
            .behandlingResultatType(behandlingsresultat == null ? null : behandlingsresultat.getBehandlingResultatType().getKode())
            .behandlingStatus(mapBehandlingStatus(behandling))
            .behandlingType(behandling.getType().getKode())
            .endretAv(CommonDvhMapper.finnEndretAvEllerOpprettetAv(behandling))
            .fagsakId(behandling.getFagsakId())
            .saksnummer(behandling.getFagsak().getSaksnummer().getVerdi())
            .aktørId(behandling.getAktørId().getId())
            .ytelseType(behandling.getFagsakYtelseType().getKode())
            .funksjonellTid(LocalDateTime.now())
            .utlandstilsnitt(getUtlandstilsnitt(fagsakMarkering))
            .relatertBehandling(getRelatertBehandling(behandling, klageResultat, ankeResultat))
            .familieHendelseType(mapFamilieHendelse(fh))
            .medFoersteStoenadsdag(skjæringstidspunkt.orElse(null))
            .medPapirSøknad(finnPapirSøknad(behandling, mottatteDokument))
            .medBehandlingMetode(utledBehandlingMetode(behandling, behandlingsresultat))
            .medRevurderingÅrsak(utledRevurderingÅrsak(behandling, fagsakMarkering))
            .medMottattTid(finnMottattTidspunkt(mottatteDokument))
            .medRegistrertTid(behandling.getOpprettetTidspunkt())
            .medKanBehandlesTid(kanBehandlesTid(behandling))
            .medFerdigBehandletTid(behandling.erAvsluttet() ? behandling.getEndretTidspunkt() : null)
            .medForventetOppstartTid(forventetOppstartDato.orElse(null));
        vedtak.ifPresent(v -> builder.vedtakTid(v.getVedtakstidspunkt())
            .vedtakResultatType(Optional.ofNullable(v.getVedtakResultatType()).map(Kodeverdi::getKode).orElse(null))
            .vilkårIkkeOppfylt(vilkårIkkeOppfylt.orElse(null))
            .utbetaltTid(utbetaltTid.orElse(null)));
        return builder.build();
    }

    private static String mapBehandlingStatus(Behandling behandling) {
        return behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).stream()
            .min(Comparator.comparing(Aksjonspunkt::getOpprettetTidspunkt))
            .map(VenteGruppe::getKategoriFor)
            .map(VenteGruppe.VenteKategori::name)
            .orElseGet(() -> behandling.getStatus().getKode());
    }

    private static String getUtlandstilsnitt(FagsakMarkering fagsakMarkering) {
        return FagsakMarkering.BOSATT_UTLAND.equals(fagsakMarkering) || FagsakMarkering.EØS_BOSATT_NORGE.equals(fagsakMarkering) ?
            fagsakMarkering.name() : FagsakMarkering.NASJONAL.name();
    }

    private static LocalDateTime kanBehandlesTid(Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD)
            .map(BehandlingDvhMapper::finnTidligste)
            .filter(t -> t.isAfter(behandling.getOpprettetTidspunkt()))
            .orElse(null);
    }

    private static LocalDateTime finnTidligste(Aksjonspunkt aksjonspunkt) {
        if (!aksjonspunkt.erOpprettet() && aksjonspunkt.getEndretTidspunkt() != null
            && aksjonspunkt.getEndretTidspunkt().isBefore(aksjonspunkt.getFristTid())) {
            return aksjonspunkt.getEndretTidspunkt();
        }
        return aksjonspunkt.getFristTid();
    }

    /**
     * Er det klage, hentes relatert behandling fra klageresultat. Hvis ikke hentes relatert behandling fra orginalbehandling-referansen på behandlingen.
     */
    private static Long getRelatertBehandling(Behandling behandling,
                                              Optional<KlageResultatEntitet> klageResultat,
                                              Optional<AnkeResultatEntitet> ankeResultat) {
        if (BehandlingType.ANKE.equals(behandling.getType()) && ankeResultat.isPresent()) {
            return ankeResultat.flatMap(AnkeResultatEntitet::getPåAnketKlageBehandlingId).orElse(null);
        }
        if (BehandlingType.KLAGE.equals(behandling.getType()) && klageResultat.isPresent()) {
            return klageResultat.flatMap(KlageResultatEntitet::getPåKlagdBehandlingId).orElse(null);
        }
        return behandling.getOriginalBehandlingId().orElse(null);
    }

    private static String mapFamilieHendelse(Optional<FamilieHendelseGrunnlagEntitet> fh) {
        return fh.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType)
            .map(FamilieHendelseType::getKode)
            .orElse(null);
    }

    private static Boolean finnPapirSøknad(Behandling behandling, List<MottattDokument> mottatteDokumenter) {
        if (!behandling.erYtelseBehandling() || mottatteDokumenter.isEmpty()) {
            return null;
        }
        return mottatteDokumenter.stream().anyMatch(md -> !md.getElektroniskRegistrert());
    }

    private static LocalDateTime finnMottattTidspunkt(List<MottattDokument> mottatteDokumenter) {
        return mottatteDokumenter.stream()
            .map(d -> d.getMottattTidspunkt().isBefore(d.getOpprettetTidspunkt()) ? d.getMottattTidspunkt() : d.getOpprettetTidspunkt())
            .min(Comparator.naturalOrder()).orElse(null);
    }

    private static BehandlingMetode utledBehandlingMetode(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (!behandling.erSaksbehandlingAvsluttet()) {
            return null;
        }
        if (behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FATTER_VEDTAK).filter(Aksjonspunkt::erUtført).isPresent()) {
            return BehandlingMetode.TOTRINN;
        }
        if (behandling.getType().erKlageAnkeType() && !behandlingsresultat.isBehandlingHenlagt()) {
            return BehandlingMetode.TOTRINN;
        }
        if (behandling.getAksjonspunkter().stream().filter(ap -> !ap.erAutopunkt()).anyMatch(BehandlingDvhMapper::harSaksbehandlerVurdertAksjonspunkt)) {
            return BehandlingMetode.MANUELL;
        }
        if (behandling.getAksjonspunkter().stream().map(Aksjonspunkt::getAksjonspunktDefinisjon).anyMatch(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT::equals)) {
            return BehandlingMetode.INNHENTING;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_SATS_REGULERING)) {
            return BehandlingMetode.REGULERING;
        }
        return BehandlingMetode.AUTOMATISK;
    }

    private static boolean harSaksbehandlerVurdertAksjonspunkt(Aksjonspunkt aksjonspunkt) {
        return aksjonspunkt.erUtført() || aksjonspunkt.getBegrunnelse() != null ||
            CommonDvhMapper.erSaksbehandler(aksjonspunkt.getEndretAv()) || CommonDvhMapper.erSaksbehandler(aksjonspunkt.getOpprettetAv());
    }

    private static RevurderingÅrsak utledRevurderingÅrsak(Behandling behandling, FagsakMarkering fagsakMarkering) {
        if (!behandling.erRevurdering()) {
            return null;
        }
        // Midlertidig
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE) || FagsakMarkering.PRAKSIS_UTSETTELSE.equals(fagsakMarkering)) {
            return RevurderingÅrsak.PRAKSISUTSETTELSE;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            return RevurderingÅrsak.SØKNAD;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_UTSATT_START)) {
            return RevurderingÅrsak.SØKNAD;
        }
        if (behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.årsakerEtterKlageBehandling())) {
            return RevurderingÅrsak.KLAGE;
        }
        if (behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.årsakerForEtterkontroll())) {
            return RevurderingÅrsak.ETTERKONTROLL;
        }
        if (behandling.erManueltOpprettet() && behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING)) {
            return RevurderingÅrsak.UTTAKMANUELL;
        }
        if (behandling.erManueltOpprettet()) {
            return RevurderingÅrsak.MANUELL;
        }
        if (behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.årsakerForRelatertVedtak())) {
            return RevurderingÅrsak.ANNENFORELDER;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN)) {
            return RevurderingÅrsak.NYSAK;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER)) {
            return RevurderingÅrsak.PLEIEPENGER;
        }
        if (behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.årsakerRelatertTilPdl())) {
            return RevurderingÅrsak.FOLKEREGISTER;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)) {
            return RevurderingÅrsak.INNTEKTSMELDING;
        }
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_SATS_REGULERING)) {
            return RevurderingÅrsak.REGULERING;
        }
        return RevurderingÅrsak.MANUELL;
    }

    // Pga konvensjon med å sette ansvarlig til null når behandling settes på vent.
    private static String utledAnsvarligSaksbehandler(Behandling behandling) {
        if (behandling.getAnsvarligSaksbehandler() != null || !behandling.isBehandlingPåVent()) {
            return behandling.getAnsvarligSaksbehandler();
        }
        if (KontekstHolder.harKontekst() && !KontekstHolder.getKontekst().getIdentType().erSystem()) {
            return KontekstHolder.getKontekst().getUid();
        }
        return behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).stream()
            .max(Comparator.comparing(ap -> Optional.ofNullable(ap.getEndretTidspunkt()).orElseGet(ap::getOpprettetTidspunkt)))
            .map(CommonDvhMapper::finnEndretAvEllerOpprettetAv)
            .filter(CommonDvhMapper::erSaksbehandler).orElse(null);
    }

}
