package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;

public class Oppdrag110Dto extends SporingDto {

    private Long fagsystemId;
    private String oppdragGjelderId;
    private String saksbehId;
    private Long oppdragsKontrollId;
    private String kodeAksjon;
    private String kodeEndring;
    private String kodeFagomrade;
    private String utbetFrekvens;
    private LocalDate datoOppdragGjelderFom;
    private AvstemmingDto avstemming;
    private Ompostering116Dto ompostering116Dto;
    private List<OppdragKvitteringDto> oppdragKvittering;
    private List<Oppdragslinje150Dto> oppdragslinje150;

    public Oppdrag110Dto(Oppdrag110 entitet) {
        super(entitet, entitet.getVersjon(), entitet.getId());
    }

    public Long getFagsystemId() {
        return fagsystemId;
    }

    public void setFagsystemId(Long fagsystemId) {
        this.fagsystemId = fagsystemId;
    }

    public String getOppdragGjelderId() {
        return oppdragGjelderId;
    }

    public void setOppdragGjelderId(String oppdragGjelderId) {
        this.oppdragGjelderId = oppdragGjelderId;
    }

    public String getSaksbehId() {
        return saksbehId;
    }

    public void setSaksbehId(String saksbehId) {
        this.saksbehId = saksbehId;
    }

    public Long getOppdragsKontrollId() {
        return oppdragsKontrollId;
    }

    public void setOppdragsKontrollId(Long oppdragsKontrollId) {
        this.oppdragsKontrollId = oppdragsKontrollId;
    }

    public String getKodeAksjon() {
        return kodeAksjon;
    }

    public void setKodeAksjon(String kodeAksjon) {
        this.kodeAksjon = kodeAksjon;
    }

    public String getKodeEndring() {
        return kodeEndring;
    }

    public void setKodeEndring(String kodeEndring) {
        this.kodeEndring = kodeEndring;
    }

    public String getKodeFagomrade() {
        return kodeFagomrade;
    }

    public void setKodeFagomrade(String kodeFagomrade) {
        this.kodeFagomrade = kodeFagomrade;
    }

    public String getUtbetFrekvens() {
        return utbetFrekvens;
    }

    public void setUtbetFrekvens(String utbetFrekvens) {
        this.utbetFrekvens = utbetFrekvens;
    }

    public AvstemmingDto getAvstemming() {
        return avstemming;
    }

    public void setAvstemming(AvstemmingDto avstemming) {
        this.avstemming = avstemming;
    }

    public List<Oppdragslinje150Dto> getOppdragslinje150() {
        return oppdragslinje150;
    }

    public void setOppdragslinje150(List<Oppdragslinje150Dto> oppdragslinje150) {
        this.oppdragslinje150 = oppdragslinje150;
    }

    public static Oppdrag110Dto fraDomene(Oppdrag110 oppdrag110) {
        Oppdrag110Dto oppdrag110Dto = new Oppdrag110Dto(oppdrag110);
        oppdrag110Dto.datoOppdragGjelderFom = oppdrag110.getDatoOppdragGjelderFom();
        oppdrag110Dto.fagsystemId = oppdrag110.getFagsystemId();
        oppdrag110Dto.oppdragGjelderId = oppdrag110.getOppdragGjelderId();
        oppdrag110Dto.saksbehId = oppdrag110.getSaksbehId();
        oppdrag110Dto.oppdragsKontrollId = oppdrag110.getOppdragskontroll().getId();
        oppdrag110Dto.kodeAksjon = oppdrag110.getKodeAksjon();
        oppdrag110Dto.kodeEndring = oppdrag110.getKodeEndring();
        oppdrag110Dto.kodeFagomrade = oppdrag110.getKodeFagomrade();
        oppdrag110Dto.utbetFrekvens = oppdrag110.getUtbetFrekvens();
        oppdrag110Dto.avstemming = AvstemmingDto.fraDomene(oppdrag110.getAvstemming());
        oppdrag110Dto.datoOppdragGjelderFom = oppdrag110.getDatoOppdragGjelderFom();

        oppdrag110Dto.oppdragKvittering = oppdrag110.erKvitteringMottatt() ?
            List.of(OppdragKvitteringDto.fraDomene(oppdrag110.getOppdragKvittering())) :
            List.of();
        Optional<Ompostering116> optOmposter = oppdrag110.getOmpostering116();
        oppdrag110Dto.oppdragslinje150 = oppdrag110.getOppdragslinje150Liste().stream()
            .map(Oppdragslinje150Dto::fraDomene)
            .collect(Collectors.toList());

        if (optOmposter.isPresent()) {
            oppdrag110Dto.ompostering116Dto = Ompostering116Dto.fraDomene(optOmposter.get());
        }
        oppdrag110Dto.oppdragsKontrollId = oppdrag110.getOppdragskontroll().getId();
        return oppdrag110Dto;
    }

    public Ompostering116Dto getOmpostering116Dto() {
        return ompostering116Dto;
    }

    public Oppdrag110Dto setOmpostering116Dto(Ompostering116Dto ompostering116Dto) {
        this.ompostering116Dto = ompostering116Dto;
        return this;
    }

    public LocalDate getDatoOppdragGjelderFom() {
        return datoOppdragGjelderFom;
    }

    public Oppdrag110Dto setDatoOppdragGjelderFom(LocalDate datoOppdragGjelderFom) {
        this.datoOppdragGjelderFom = datoOppdragGjelderFom;
        return this;
    }

    public List<OppdragKvitteringDto> getOppdragKvittering() {
        return oppdragKvittering;
    }

}
