import { useState } from "react";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "./ui/sheet";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Progress } from "./ui/progress";
import { Download, FileText, CheckCircle2, Clock, Loader2 } from "lucide-react";
import { FileRecord, api } from "../api";

type FrontendStatus = "pending" | "uploaded" | "processing" | "completed" | "failed";

const statusMap: Record<string, FrontendStatus> = {
  PENDING_UPLOAD: "pending",
  UPLOADED: "uploaded",
  PROCESSING: "processing",
  COMPLETED: "completed",
  FAILED: "failed",
};

const statusColors: Record<FrontendStatus, string> = {
  pending:    "bg-gray-500/10 text-gray-400 border-gray-500/20",
  uploaded:   "bg-blue-500/10 text-blue-400 border-blue-500/20",
  processing: "bg-yellow-500/10 text-yellow-400 border-yellow-500/20",
  completed:  "bg-green-500/10 text-green-400 border-green-500/20",
  failed:     "bg-red-500/10 text-red-400 border-red-500/20",
};

function formatBytes(bytes: number | null): string {
  if (!bytes) return "—";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${Math.round((bytes / Math.pow(k, i)) * 10) / 10} ${sizes[i]}`;
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("en-US", {
    month: "short", day: "numeric", year: "numeric",
    hour: "numeric", minute: "2-digit", hour12: true,
  });
}

interface JobDetailDrawerProps {
  file: FileRecord | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function JobDetailDrawer({ file, open, onOpenChange }: JobDetailDrawerProps) {
  const [downloading, setDownloading] = useState(false);

  if (!file) return null;

  const status = statusMap[file.status] ?? "pending";

  const compressionRatio =
    file.outputSizeBytes && file.sizeBytes
      ? Math.round(((file.sizeBytes - file.outputSizeBytes) / file.sizeBytes) * 100)
      : null;

  const handleDownload = async () => {
    setDownloading(true);
    try {
      const { downloadUrl } = await api.files.getDownloadUrl(file.id);
      window.open(downloadUrl, "_blank");
    } catch (err: any) {
      alert(err.message);
    } finally {
      setDownloading(false);
    }
  };

  const statusIcon = () => {
    switch (status) {
      case "completed":  return <CheckCircle2 className="w-5 h-5 text-green-400" />;
      case "processing": return <Loader2 className="w-5 h-5 text-yellow-400 animate-spin" />;
      default:           return <Clock className="w-5 h-5 text-muted-foreground" />;
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="bg-card border-border w-full sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>File Details</SheetTitle>
        </SheetHeader>

        <div className="mt-6 space-y-6">
          {/* File name */}
          <div className="flex items-start gap-3 p-4 bg-secondary/30 rounded-lg border border-border">
            <FileText className="w-8 h-8 text-muted-foreground mt-1 shrink-0" />
            <div className="flex-1 min-w-0">
              <h3 className="font-medium truncate">{file.originalName}</h3>
              <p className="text-sm text-muted-foreground mt-1">{file.contentType}</p>
            </div>
          </div>

          {/* Status */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">Status</span>
              <div className="flex items-center gap-2">
                {statusIcon()}
                <Badge variant="outline" className={`capitalize ${statusColors[status]}`}>
                  {status}
                </Badge>
              </div>
            </div>
            {status === "processing" && (
              <div className="space-y-2">
                <Progress value={65} className="h-2" />
                <p className="text-xs text-muted-foreground">Compressing file…</p>
              </div>
            )}
          </div>

          {/* Sizes */}
          <div className="space-y-4 p-4 bg-secondary/30 rounded-lg border border-border">
            <div className="flex justify-between items-center">
              <span className="text-sm text-muted-foreground">Original Size</span>
              <span className="text-sm font-medium">{formatBytes(file.sizeBytes)}</span>
            </div>

            {file.outputSizeBytes && (
              <>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-muted-foreground">Compressed Size</span>
                  <span className="text-sm font-medium text-green-400">
                    {formatBytes(file.outputSizeBytes)}
                  </span>
                </div>
                {compressionRatio !== null && (
                  <div className="pt-3 border-t border-border flex justify-between items-center">
                    <span className="text-sm text-muted-foreground">Space Saved</span>
                    <span className="text-sm font-medium text-green-400">
                      {compressionRatio}% smaller
                    </span>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Timestamps */}
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-sm text-muted-foreground">Uploaded At</span>
              <span className="text-sm">{formatDateTime(file.createdAt)}</span>
            </div>
            {status === "completed" && (
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">Completed At</span>
                <span className="text-sm">{formatDateTime(file.updatedAt)}</span>
              </div>
            )}
          </div>

          {/* Download */}
          {status === "completed" && (
            <Button
              className="w-full bg-primary hover:bg-primary/90"
              onClick={handleDownload}
              disabled={downloading}
            >
              {downloading
                ? <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                : <Download className="w-4 h-4 mr-2" />}
              Download Compressed File
            </Button>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
