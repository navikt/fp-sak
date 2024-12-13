package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;

public class DatavarehusTestUtils {
    protected static final String ANSVARLIG_BESLUTTER = "ansvarligBeslutter";
    protected static final String ANSVARLIG_SAKSBEHANDLER = "ansvarligSaksbehandler";
    protected static final String BEHANDLENDE_ENHET = "behandlendeEnhet";
    protected static final long BEHANDLING_ID = 600L;
    protected static final String BEHANDLING_RESULTAT_TYPE = "behandlingResultatType";
    protected static final String BEHANDLING_STATUS = "behandlingStatus";
    protected static final String BEHANDLING_TYPE = "behandlingType";
    protected static final String BRUKER_AKTØR_ID = "55";
    protected static final String ENDRET_AV = "endret_av";
    protected static final long FAGSAK_ID = 342L;
    protected static final String FAGSAK_TYPE = "FP";
    protected static final String FAGSAK_YTELSE = "fagsakYtelse";
    protected static final LocalDateTime FUNKSJONELL_TID = LocalDateTime.now();
    protected static final LocalDateTime MOTTATT_TID = LocalDateTime.now().minusDays(1);
    protected static final LocalDate OPPRETTET_DATE = LocalDate.now();
    protected static final long SAKSNUMMER = 442L;
    protected static final String UTLANDSTILSNITT = "utlandstilsnitt";
    protected static final LocalDate VEDTAK_DATO = LocalDate.now();
    protected static final LocalDateTime VEDTAK_TID = LocalDateTime.now();
    protected static final long VEDTAK_ID = 700L;
    protected static final String VEDTAK_RESULTAT_TYPE = "INNVILGET";
    protected static final String FAMILIE_HENDELSE_TYPE = FamilieHendelseType.FØDSEL.getKode();
    protected static final String SOEKNAD_TYPE = "TERM";
    protected static final String VEDTAK_XML = "<personOpplysninger> </personOpplysninger>";
    protected static final UUID BEHANDLING_UUID = UUID.randomUUID();



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
                .fagsakId(FAGSAK_ID)
                .saksnummer(String.valueOf(SAKSNUMMER))
                .aktørId(BRUKER_AKTØR_ID)
                .ytelseType(FAGSAK_YTELSE)
                .funksjonellTid(FUNKSJONELL_TID)
                .utlandstilsnitt(UTLANDSTILSNITT)
                .familieHendelseType(FamilieHendelseType.FØDSEL.getKode())
                .relatertBehandlingUuid(BEHANDLING_UUID)
                .medPapirSøknad(false)
                .medBehandlingMetode(BehandlingMetode.AUTOMATISK)
                .medRevurderingÅrsak(RevurderingÅrsak.SØKNAD)
                .medMottattTid(MOTTATT_TID)
                .medRegistrertTid(FUNKSJONELL_TID)
                .medKanBehandlesTid(FUNKSJONELL_TID.plusSeconds(1))
                .medFerdigBehandletTid(FUNKSJONELL_TID.plusMinutes(1))
                .medFoersteStoenadsdag(OPPRETTET_DATE.plusDays(1))
                .medForventetOppstartTid(OPPRETTET_DATE.plusDays(2))
                .vedtakResultatType(VEDTAK_RESULTAT_TYPE)
                .vilkårIkkeOppfylt(null)
                .vedtakTid(VEDTAK_TID)
                .utbetaltTid(VEDTAK_DATO.plusWeeks(1))
                .build();
    }

}
