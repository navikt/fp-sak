package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;

public class DatavarehusTestUtils {
    protected static final String AKSJONSPUNKT_DEF = "aksjonspunktDef";
    protected static final long AKSJONSPUNKT_ID = 900L;
    protected static final String AKSJONSPUNKT_STATUS = "aksjonspunktStatus";
    protected static final String ANSVARLIG_BESLUTTER = "ansvarligBeslutter";
    protected static final String ANSVARLIG_SAKSBEHANDLER = "ansvarligSaksbehandler";
    protected static final String BEHANDLENDE_ENHET = "behandlendeEnhet";
    protected static final long BEHANDLING_ID = 600L;
    protected static final String BEHANDLING_RESULTAT_TYPE = "behandlingResultatType";
    protected static final String BEHANDLING_STATUS = "behandlingStatus";
    protected static final long BEHANDLING_STEG_ID = 800L;
    protected static final String BEHANDLING_TYPE = "behandlingType";
    protected static final String BRUKER_AKTØR_ID = "55";
    protected static final long BRUKER_ID = 142L;
    protected static final String ENDRET_AV = "endret_av";
    protected static final String EPS_AKTØR_ID = "242";
    protected static final long FAGSAK_ID = 342L;
    protected static final String FAGSAK_STATUS = "fagsakStatus";
    protected static final String FAGSAK_TYPE = "FP";
    protected static final String FAGSAK_YTELSE = "fagsakYtelse";
    protected static final LocalDateTime FUNKSJONELL_TID = LocalDateTime.now();
    protected static final String GODKJENNENDE_ENHET = "godkjennendeEnhet";
    protected static final String IVERKSETTING_STATUS = "iverksettingStatus";
    protected static final LocalDateTime MOTTATT_TID = LocalDateTime.now().minusDays(1);
    protected static final LocalDate OPPRETTET_DATE = LocalDate.now();
    protected static final long SAKSNUMMER = 442L;
    protected static final String UTLANDSTILSNITT = "utlandstilsnitt";
    protected static final LocalDate VEDTAK_DATO = LocalDate.now();
    protected static final LocalDateTime VEDTAK_TID = LocalDateTime.now();
    protected static final long VEDTAK_ID = 700L;
    protected static final String VEDTAK_RESULTAT_TYPE = "INNVILGET";
    protected static final String SOEKNAD_FAMILIE_HENDELSE = "SOEKNAD_FAMILIE_HENDELSE";
    protected static final String BEKREFTET_FAMILIE_HENDELSE = "BEKREFTET_FAMILIE_HENDELSE";
    protected static final String OVERSTYRT_FAMILIE_HENDELSE = "OVERSTYRT_FAMILIE_HENDELSE";
    protected static final String SOEKNAD_TYPE = "TERM";
    protected static final String VEDTAK_XML = "<personOpplysninger> </personOpplysninger>";
    protected static final LocalDate AVSLUTTNINGS_DATO = LocalDate.now().plusDays(100);
    protected static final UUID BEHANDLING_UUID = UUID.randomUUID();



    public static AksjonspunktDvh byggAksjonspunktDvh() {
        return AksjonspunktDvh.builder()
                .aksjonspunktDef(AKSJONSPUNKT_DEF)
                .aksjonspunktId(AKSJONSPUNKT_ID)
                .aksjonspunktStatus(AKSJONSPUNKT_STATUS)
                .ansvarligBeslutter(ANSVARLIG_BESLUTTER)
                .ansvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
                .behandlendeEnhetKode(BEHANDLENDE_ENHET)
                .behandlingId(BEHANDLING_ID)
                .behandlingStegId(BEHANDLING_STEG_ID)
                .endretAv(ENDRET_AV)
                .funksjonellTid(FUNKSJONELL_TID)
                .toTrinnsBehandling(true)
                .toTrinnsBehandlingGodkjent(true)
                .build();
    }

    public static BehandlingDvh byggBehandlingDvh() {
        return BehandlingDvh.builder()
                .ansvarligBeslutter(ANSVARLIG_BESLUTTER)
                .ansvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
                .behandlendeEnhet(BEHANDLENDE_ENHET)
                .behandlingResultatType(BEHANDLING_RESULTAT_TYPE)
                .behandlingId(BEHANDLING_ID)
                .behandlingUuid(BEHANDLING_UUID)
                .behandlingStatus(BEHANDLING_STATUS)
                .behandlingType(BEHANDLING_TYPE)
                .endretAv(ENDRET_AV)
                .fagsakId(FAGSAK_ID)
                .funksjonellTid(FUNKSJONELL_TID)
                .opprettetDato(OPPRETTET_DATE)
                .utlandstilsnitt(UTLANDSTILSNITT)
                .toTrinnsBehandling(true)
                .vedtakId(VEDTAK_ID)
                .soeknadFamilieHendelse(SOEKNAD_FAMILIE_HENDELSE)
                .bekreftetFamilieHendelse(BEKREFTET_FAMILIE_HENDELSE)
                .overstyrtFamilieHendelse(OVERSTYRT_FAMILIE_HENDELSE)
                .relatertBehandling(BEHANDLING_ID)
                .medPapirSøknad(false)
                .medBehandlingMetode(BehandlingMetode.AUTOMATISK)
                .medRevurderingÅrsak(RevurderingÅrsak.SØKNAD)
                .medMottattTidspunkt(MOTTATT_TID)
                .medMottattTid(MOTTATT_TID)
                .medRegistrertTid(FUNKSJONELL_TID)
                .medKanBehandlesTid(FUNKSJONELL_TID.plusSeconds(1))
                .medFerdigBehandletTid(FUNKSJONELL_TID.plusMinutes(1))
                .medFoersteStoenadsdag(OPPRETTET_DATE.plusDays(1))
                .medForventetOppstartTid(OPPRETTET_DATE.plusDays(2))
                .build();
    }

    public static BehandlingVedtakDvh byggBehandlingVedtakDvh() {
        return BehandlingVedtakDvh.builder()
                .ansvarligBeslutter(ANSVARLIG_BESLUTTER)
                .ansvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
                .behandlingId(BEHANDLING_ID)
                .endretAv(ENDRET_AV)
                .funksjonellTid(FUNKSJONELL_TID)
                .godkjennendeEnhet(GODKJENNENDE_ENHET)
                .iverksettingStatus(IVERKSETTING_STATUS)
                .opprettetDato(OPPRETTET_DATE)
                .vedtakDato(VEDTAK_DATO)
                .vedtakId(VEDTAK_ID)
                .vedtakResultatTypeKode(VEDTAK_RESULTAT_TYPE)
                .vedtakTid(VEDTAK_TID)
                .utbetaltTid(VEDTAK_DATO.plusWeeks(1))
                .build();
    }

    public static FagsakDvh byggFagsakDvhForTest() {
        return FagsakDvh.builder().brukerAktørId("42")
                .brukerId(BRUKER_ID)
                .brukerAktørId(BRUKER_AKTØR_ID)
                .endretAv(ENDRET_AV)
                .epsAktørId(Optional.of(EPS_AKTØR_ID))
                .fagsakId(FAGSAK_ID)
                .fagsakStatus(FAGSAK_STATUS)
                .fagsakYtelse(FAGSAK_YTELSE)
                .funksjonellTid(FUNKSJONELL_TID)
                .opprettetDato(OPPRETTET_DATE)
                .saksnummer(SAKSNUMMER)
                .build();
    }

    public static VedtakUtbetalingDvh byggVedtakUtbetalingDvh() {
        return VedtakUtbetalingDvh.builder()
            .behandlingId(BEHANDLING_ID)
            .behandlingType(BEHANDLING_TYPE)
            .endretAv(ENDRET_AV)
            .fagsakId(FAGSAK_ID)
            .fagsakType(FAGSAK_TYPE)
            .søknadType(SOEKNAD_TYPE)
            .vedtakDato(VEDTAK_DATO)
            .vedtakId(VEDTAK_ID)
            .xmlClob(VEDTAK_XML)
            .build();
    }

    public static FagsakRelasjonDvh byggFagsakRelasjonDvhForTest() {
        return FagsakRelasjonDvh.builder()
            .fagsakNrEn(FAGSAK_ID)
            .fagsakNrTo(FAGSAK_ID)
            .dekningsgrad(Dekningsgrad._100)
            .avsluttningsdato(AVSLUTTNINGS_DATO)
            .endretAv(ENDRET_AV)
            .funksjonellTid(FUNKSJONELL_TID)
            .build();
    }
}
