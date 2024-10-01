package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "SvpGrunnlag")
@Table(name = "SVP_GRUNNLAG")
public class SvpGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SVP_GRUNNLAG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "OPPRINNELINGE_TRLG_ID", updatable = false, unique = true)
    private SvpTilretteleggingerEntitet opprinneligeTilrettelegginger;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "OVERSTYRTE_TRLG_ID", updatable = false, unique = true)
    private SvpTilretteleggingerEntitet overstyrteTilrettelegginger;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    public SvpTilretteleggingerEntitet getOpprinneligeTilrettelegginger() {
        return opprinneligeTilrettelegginger;
    }

    public SvpTilretteleggingerEntitet getOverstyrteTilrettelegginger() {
        return overstyrteTilrettelegginger;
    }

    public SvpTilretteleggingerEntitet getGjeldendeVersjon() {
        if (getOverstyrteTilrettelegginger() != null && !getOverstyrteTilrettelegginger().getTilretteleggingListe().isEmpty()) {
            return overstyrteTilrettelegginger;
        }
        return opprinneligeTilrettelegginger;
    }

    public List<SvpTilretteleggingEntitet> hentTilretteleggingerSomSkalBrukes() {
        return getGjeldendeVersjon().getTilretteleggingListe().stream()
            .filter(SvpTilretteleggingEntitet::getSkalBrukes)
            .toList();
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void deaktiver() {
        this.aktiv = false;
    }

    public static class Builder {

        private List<SvpTilretteleggingEntitet> opprinneligeTilretteleggingListe = new ArrayList<>();
        private List<SvpTilretteleggingEntitet> overstyrteTilretteleggingerListe = new ArrayList<>();

        private SvpTilretteleggingerEntitet opprinneligeTilrettelegginger;
        private SvpTilretteleggingerEntitet overstyrteTilrettelegginger;

        private Long behandlingId;

        public Builder() {
        }

        public Builder(SvpGrunnlagEntitet eksisterendeGrunnlag) {
            this.behandlingId = eksisterendeGrunnlag.behandlingId;
            this.opprinneligeTilrettelegginger = eksisterendeGrunnlag.opprinneligeTilrettelegginger;
            this.overstyrteTilrettelegginger = eksisterendeGrunnlag.overstyrteTilrettelegginger;

        }

        public Builder medBehandlingId(Long behandlingId) {
            this.behandlingId = behandlingId;
            return this;
        }

        public Builder medOpprinneligeTilrettelegginger(List<SvpTilretteleggingEntitet> tilrettelegginger) {
            this.opprinneligeTilretteleggingListe = tilrettelegginger;
            return this;
        }

        public Builder medOverstyrteTilrettelegginger(List<SvpTilretteleggingEntitet> tilrettelegginger) {
            this.overstyrteTilretteleggingerListe = tilrettelegginger;
            return this;
        }

        public SvpGrunnlagEntitet build() {
            Objects.requireNonNull(behandlingId, "Behandling er påkrevet");

            var entitet = new SvpGrunnlagEntitet();
            entitet.behandlingId = this.behandlingId;
            entitet.aktiv = true;

            if (this.opprinneligeTilrettelegginger != null) {
                entitet.opprinneligeTilrettelegginger = opprinneligeTilrettelegginger;
            } else if (!opprinneligeTilretteleggingListe.isEmpty()) {
                var opprinneligeTrlgBuilder = new SvpTilretteleggingerEntitet.Builder();
                opprinneligeTrlgBuilder.medTilretteleggingListe(this.opprinneligeTilretteleggingListe);
                entitet.opprinneligeTilrettelegginger = opprinneligeTrlgBuilder.build();
            }

            if  (overstyrteTilretteleggingerListe != null && !overstyrteTilretteleggingerListe.isEmpty()) {
                var overstyrteTrlgBuilder = new SvpTilretteleggingerEntitet.Builder();
                overstyrteTrlgBuilder.medTilretteleggingListe(this.overstyrteTilretteleggingerListe);
                entitet.overstyrteTilrettelegginger = overstyrteTrlgBuilder.build();
            } else if (overstyrteTilretteleggingerListe != null && this.overstyrteTilrettelegginger != null) {
                entitet.overstyrteTilrettelegginger = overstyrteTilrettelegginger;
            }


            return entitet;
        }
    }
}
